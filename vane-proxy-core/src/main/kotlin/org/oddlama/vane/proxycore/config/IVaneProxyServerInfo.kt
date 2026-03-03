package org.oddlama.vane.proxycore.config

import java.net.SocketAddress

interface IVaneProxyServerInfo {
    val name: String?

    val socketAddress: SocketAddress?

    fun sendData(data: ByteArray?)

    fun sendData(data: ByteArray?, queue: Boolean): Boolean
}
