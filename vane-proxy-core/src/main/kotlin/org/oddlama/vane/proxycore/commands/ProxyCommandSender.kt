package org.oddlama.vane.proxycore.commands

/**
 * Represents a source capable of receiving command feedback.
 */
interface ProxyCommandSender {
    /**
     * Sends a chat/system message to this sender.
     *
     * @param message formatted message content.
     */
    fun sendMessage(message: String?)
}
