package org.zeroshare.app.domain.model

import org.zeroshare.app.core.security.ConnectionType

data class PeerDevice(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = 8988,
    val connectionType: ConnectionType,
    val isGroupOwner: Boolean = false,
    val signalStrength: Int = 0
)
