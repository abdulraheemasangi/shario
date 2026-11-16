#!/usr/bin/env python3
"""
Shario Quality & Protocol Verification Suite
Simulates sender and receiver over raw TCP sockets:
1. Generates 48MP RAW photo and 4K video test payloads.
2. Runs custom wire protocol (Magic 0x53, Handshake, Manifest, Chunks, File Complete, Checksum Ack).
3. Verifies byte-for-byte identical output and matching SHA-256 digests.
"""

import hashlib
import json
import os
import socket
import struct
import tempfile
import threading
import time

MAGIC_BYTE = 0x53  # 'S' for Shario
TYPE_HANDSHAKE = 0x01
TYPE_MANIFEST = 0x02
TYPE_RESUME_REQUEST = 0x03
TYPE_RESUME_RESPONSE = 0x04
TYPE_CHUNK_DATA = 0x05
TYPE_FILE_COMPLETE = 0x06
TYPE_VERIFICATION_ACK = 0x07
TYPE_CANCEL = 0x08

CHUNK_SIZE = 1024 * 1024  # 1 MB


def write_packet(sock, packet_type, file_id=0, chunk_index=0, payload=b""):
    header = struct.pack(">B B i i i", MAGIC_BYTE, packet_type, file_id, chunk_index, len(payload))
    sock.sendall(header + payload)


def read_packet(sock):
    header_data = b""
    while len(header_data) < 14:
        chunk = sock.recv(14 - len(header_data))
        if not chunk:
            raise EOFError("Socket closed")
        header_data += chunk

    magic, packet_type, file_id, chunk_index, length = struct.unpack(">B B i i i", header_data)
    if magic != MAGIC_BYTE:
        raise ValueError(f"Invalid magic byte: {magic:#x}")

    payload = b""
    while len(payload) < length:
        chunk = sock.recv(length - len(payload))
        if not chunk:
            raise EOFError("Unexpected EOF reading payload")
        payload += chunk

    return packet_type, file_id, chunk_index, payload


def receiver_server(port, ready_event, result_holder):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("127.0.0.1", port))
    server.listen(1)
    ready_event.set()

    conn, _ = server.accept()
    received_files = {}

    try:
        # 1. Handshake
        ptype, _, _, payload = read_packet(conn)
        assert ptype == TYPE_HANDSHAKE

        # 2. Manifest
        ptype, _, _, payload = read_packet(conn)
        assert ptype == TYPE_MANIFEST
        manifest = json.loads(payload.decode("utf-8"))

        for file_meta in manifest["files"]:
            file_id = file_meta["fileId"]
            file_name = file_meta["name"]
            expected_hash = file_meta["sha256"]
            total_size = file_meta["size"]

            # Resume Request
            ptype, _, _, _ = read_packet(conn)
            assert ptype == TYPE_RESUME_REQUEST

            # Send Resume Response (offset 0)
            resume_resp = json.dumps({"fileId": file_id, "offset": 0, "chunkIndex": 0}).encode("utf-8")
            write_packet(conn, TYPE_RESUME_RESPONSE, payload=resume_resp)

            hasher = hashlib.sha256()
            received_bytes = bytearray()

            while True:
                ptype, f_id, c_idx, chunk_data = read_packet(conn)
                if ptype == TYPE_CHUNK_DATA:
                    hasher.update(chunk_data)
                    received_bytes.extend(chunk_data)
                elif ptype == TYPE_FILE_COMPLETE:
                    break

            actual_hash = hasher.hexdigest()
            is_verified = (actual_hash.lower() == expected_hash.lower())

            # Verification ACK
            ack_json = json.dumps({
                "fileId": file_id,
                "status": "VERIFIED" if is_verified else "MISMATCH",
                "actualHash": actual_hash
            }).encode("utf-8")
            write_packet(conn, TYPE_VERIFICATION_ACK, payload=ack_json)

            received_files[file_name] = {
                "size": len(received_bytes),
                "actual_hash": actual_hash,
                "expected_hash": expected_hash,
                "verified": is_verified
            }

        result_holder["received"] = received_files
    finally:
        conn.close()
        server.close()


def run_verification():
    print("=" * 65)
    print("      Shario Quality & Wire Protocol Verification Suite")
    print("=" * 65)

    with tempfile.TemporaryDirectory() as temp_dir:
        # 1. Create simulated 48MP RAW Photo (8MB slice) and 4K Video (16MB slice)
        photo_path = os.path.join(temp_dir, "IMG_48MP_RAW.dng")
        video_path = os.path.join(temp_dir, "VID_4K_60FPS.mp4")

        print("[*] Generating 48MP RAW photo test file (8 MB)...")
        photo_data = os.urandom(8 * 1024 * 1024)
        with open(photo_path, "wb") as f:
            f.write(photo_data)
        photo_hash = hashlib.sha256(photo_data).hexdigest()

        print("[*] Generating 4K 60fps video test file (16 MB)...")
        video_data = os.urandom(16 * 1024 * 1024)
        with open(video_path, "wb") as f:
            f.write(video_data)
        video_hash = hashlib.sha256(video_data).hexdigest()

        print(f"    Source 48MP Photo SHA-256: {photo_hash}")
        print(f"    Source 4K Video   SHA-256: {video_hash}")

        # Start Receiver thread
        port = 8999
        ready = threading.Event()
        results = {}
        server_thread = threading.Thread(target=receiver_server, args=(port, ready, results))
        server_thread.start()
        ready.wait()

        # Connect Sender
        print("\n[*] Connecting Sender to Receiver on localhost:8999...")
        sender_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sender_sock.connect(("127.0.0.1", port))

        # Handshake
        handshake_data = json.dumps({"protocol": 1, "device": "SenderPhone"}).encode("utf-8")
        write_packet(sender_sock, TYPE_HANDSHAKE, payload=handshake_data)

        # Manifest
        manifest = {
            "transferId": "test-transfer-001",
            "senderDeviceName": "Pixel 9 Pro",
            "totalFiles": 2,
            "totalBytes": len(photo_data) + len(video_data),
            "files": [
                {
                    "fileId": 1,
                    "name": "IMG_48MP_RAW.dng",
                    "size": len(photo_data),
                    "mimeType": "image/x-adobe-dng",
                    "sha256": photo_hash,
                    "chunkSize": CHUNK_SIZE
                },
                {
                    "fileId": 2,
                    "name": "VID_4K_60FPS.mp4",
                    "size": len(video_data),
                    "mimeType": "video/mp4",
                    "sha256": video_hash,
                    "chunkSize": CHUNK_SIZE
                }
            ]
        }
        write_packet(sender_sock, TYPE_MANIFEST, payload=json.dumps(manifest).encode("utf-8"))

        # Send both files
        for item in [("IMG_48MP_RAW.dng", photo_path, 1), ("VID_4K_60FPS.mp4", video_path, 2)]:
            name, path, f_id = item
            print(f"\n[*] Streaming '{name}' in 1MB chunks...")

            # Request resume
            write_packet(sender_sock, TYPE_RESUME_REQUEST, file_id=f_id)
            ptype, _, _, payload = read_packet(sender_sock)
            assert ptype == TYPE_RESUME_RESPONSE

            with open(path, "rb") as f:
                c_idx = 0
                while True:
                    chunk = f.read(CHUNK_SIZE)
                    if not chunk:
                        break
                    write_packet(sender_sock, TYPE_CHUNK_DATA, file_id=f_id, chunk_index=c_idx, payload=chunk)
                    c_idx += 1

            write_packet(sender_sock, TYPE_FILE_COMPLETE, file_id=f_id)

            # Read verification ack
            ptype, _, _, ack_payload = read_packet(sender_sock)
            ack = json.loads(ack_payload.decode("utf-8"))
            print(f"    Verification ACK received: Status={ack['status']}, Hash={ack['actualHash'][:16]}...")
            assert ack["status"] == "VERIFIED"

        sender_sock.close()
        server_thread.join()

        # Final Verification Assertions
        print("\n" + "=" * 65)
        print("                  VERIFICATION RESULTS")
        print("=" * 65)
        for fname, data in results["received"].items():
            print(f"File: {fname}")
            print(f"  - Size: {data['size']} bytes")
            print(f"  - Sender SHA-256:   {data['expected_hash']}")
            print(f"  - Receiver SHA-256: {data['actual_hash']}")
            print(f"  - Result: {'✓ VERIFIED (Byte-for-byte identical)' if data['verified'] else '✗ MISMATCH'}")
            assert data["verified"] is True
            print("-" * 65)

        print("\n[SUCCESS] Quality test passed with 100% byte fidelity!")


if __name__ == "__main__":
    run_verification()
