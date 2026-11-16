# Contributing to Shario

Thank you for your interest in contributing to Shario! This project is 100% open-source, ad-free, and dedicated to high-speed, zero-loss, byte-for-byte identical file sharing.

**Shario — Fast Sharing. Zero Hassle.**

## Code of Conduct
- Be respectful, constructive, and open to feedback.
- We have zero tolerance for tracking, telemetry, ads, or backdoors. Any PR attempting to add analytics, third-party advertising SDKs, or compression will be rejected immediately.

## Quality Invariants
All contributions must adhere to our non-negotiable rules:
1. **Never Compress**: Files must never be transcoded, resized, or downsampled.
2. **Metadata Preservation**: EXIF (including GPS with `ACCESS_MEDIA_LOCATION`), creation/modification timestamps, and file permissions must be retained.
3. **Integrity First**: Transfers must verify SHA-256 digests.
4. **Streaming Architecture**: Never read entire multi-gigabyte files into RAM. Always stream in chunks.

## Development Workflow
1. Fork the repository on GitHub.
2. Create a feature branch (`git checkout -b feature/awesome-feature`).
3. Follow Google's Kotlin style guidelines and Clean Architecture conventions.
4. Add unit tests for any new network, crypto, or file handling code.
5. Ensure all tests pass:
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```
6. Submit a Pull Request with a clear description and testing evidence.
