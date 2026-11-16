package org.zeroshare.app.core.security

import org.json.JSONObject

/**
 * Model and JSON serializer/deserializer for QR code pairing handshake.
 */
data class QrConnectionPayload(
    val version: Int = 1,
    val connectionType: ConnectionType,
    val ssid: String? = null,
    val passphrase: String? = null,
    val ipAddress: String,
    val port: Int,
    val aesKeyBase64: String,
    val receiverDeviceName: String
)

enum class ConnectionType {
    WIFI_DIRECT,
    LOCAL_HOTSPOT,
    SAME_NETWORK
}

object QrPayloadCodec {

    fun encode(payload: QrConnectionPayload): String {
        val json = JSONObject().apply {
            put("v", payload.version)
            put("type", payload.connectionType.name)
            put("ssid", payload.ssid ?: "")
            put("pass", payload.passphrase ?: "")
            put("ip", payload.ipAddress)
            put("port", payload.port)
            put("key", payload.aesKeyBase64)
            put("dev", payload.receiverDeviceName)
        }
        return json.toString()
    }

    fun decode(rawJson: String): QrConnectionPayload {
        val json = JSONObject(rawJson)
        return QrConnectionPayload(
            version = json.optInt("v", 1),
            connectionType = ConnectionType.valueOf(json.getString("type")),
            ssid = json.optString("ssid").takeIf { it.isNotEmpty() },
            passphrase = json.optString("pass").takeIf { it.isNotEmpty() },
            ipAddress = json.getString("ip"),
            port = json.getInt("port"),
            aesKeyBase64 = json.getString("key"),
            receiverDeviceName = json.optString("dev", "Unknown Receiver")
        )
    }
}
