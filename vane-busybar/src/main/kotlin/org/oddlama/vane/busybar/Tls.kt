package org.oddlama.vane.busybar

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/**
 * TLS for the event stream, in the mode set by [BusyBar.configTls]:
 *
 * - `auto`: a self-signed certificate generated on first start and kept in the data folder.
 *   Pairing strings carry the SHA-256 of its public key, which bridges pin instead of trusting a
 *   certificate authority. Needs no domain, no open ports besides the stream's, and no renewal.
 * - `keystore`: a PKCS#12 keystore the admin provides, for example converted from Let's Encrypt.
 * - `off`: plain HTTP, for a TLS reverse proxy in front of the port.
 *
 * @param busybar owning module.
 */
class Tls(private val busybar: BusyBar) {
    /** How the event stream is secured. */
    enum class Mode { AUTO, KEYSTORE, OFF }

    /** Mode the event stream was last started with. */
    @Volatile
    var mode: Mode = Mode.OFF
        private set

    /** Base64url SHA-256 of the auto certificate's public key while in [Mode.AUTO], otherwise null. */
    @Volatile
    var pin: String? = null
        private set

    /** Mode named in the configuration, falling back to [Mode.AUTO] for unknown values. */
    fun configuredMode(): Mode = parseMode(busybar.configTls) ?: Mode.AUTO

    /** Logs configurations that leave bridges unprotected or unable to connect. Call when enabling. */
    fun checkConfiguration() {
        val configured = parseMode(busybar.configTls)
        if (configured == null) {
            busybar.log.warning("Unknown Tls mode '${busybar.configTls}', using auto. Valid modes: auto, keystore, off.")
        }
        if (!busybar.server.serverConfig.isProxyOnlineMode) {
            busybar.log.warning(
                "The server runs in offline mode, so Minecraft traffic is unencrypted. Pairing strings from " +
                    "/busybar link, which hold the token and the key fingerprint, can be read or altered on the way."
            )
        }
    }

    /**
     * Builds the TLS context for the configured mode and updates [mode] and [pin].
     *
     * @return the context, or null to serve plain HTTP.
     * @throws IOException if a key or keystore file cannot be read or written.
     * @throws GeneralSecurityException if a key, certificate or keystore is invalid.
     */
    fun load(): SSLContext? {
        pin = null
        mode = configuredMode()
        return when (mode) {
            Mode.OFF -> null
            Mode.KEYSTORE -> loadKeystore()
            Mode.AUTO -> {
                val (key, certificate) = autoIdentity()
                pin = pinOf(certificate)
                val password = CharArray(0)
                val keyStore = KeyStore.getInstance("PKCS12").apply {
                    load(null, null)
                    setKeyEntry("busybar", key, password, arrayOf(certificate))
                }
                contextFor(keyStore, password)
            }
        }
    }

    /** Deletes the auto certificate, so the next [load] generates a new key. */
    fun deleteAutoIdentity() {
        autoKeyFile().delete()
        autoCertificateFile().delete()
    }

    /** Loads the admin's PKCS#12 keystore. */
    private fun loadKeystore(): SSLContext {
        val path = busybar.configTlsKeystore?.takeIf { it.isNotBlank() }
            ?: throw GeneralSecurityException("Tls is 'keystore' but TlsKeystore is empty")
        val password = busybar.configTlsKeystorePassword.orEmpty().toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12")
        File(path).inputStream().use { keyStore.load(it, password) }
        return contextFor(keyStore, password)
    }

    /** Reads the auto certificate and its key, generating both when either is missing. */
    private fun autoIdentity(): Pair<PrivateKey, X509Certificate> {
        val keyFile = autoKeyFile()
        val certificateFile = autoCertificateFile()
        if (keyFile.isFile && certificateFile.isFile) {
            val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(readPem(keyFile)))
            val certificate = certificateFile.inputStream().use {
                CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            }
            return key to certificate
        }

        val (keys, certificate) = SelfSignedCertificate.generate(COMMON_NAME)
        busybar.dataFolder.mkdirs()
        writePem(keyFile, "PRIVATE KEY", keys.private.encoded, private = true)
        writePem(certificateFile, "CERTIFICATE", certificate.encoded, private = false)
        busybar.log.info("Generated the BUSY Bar TLS certificate, key fingerprint sha256=${pinOf(certificate)}")
        return keys.private to certificate
    }

    private fun autoKeyFile() = File(busybar.dataFolder, KEY_FILE)

    private fun autoCertificateFile() = File(busybar.dataFolder, CERTIFICATE_FILE)

    companion object {
        /** Private key of the auto certificate, PKCS#8 PEM, readable by the server user only. */
        private const val KEY_FILE = "tls-auto.key"

        /** The auto certificate, PEM. */
        private const val CERTIFICATE_FILE = "tls-auto.crt"

        /** Subject of the auto certificate; bridges pin the key, so it is informational only. */
        private const val COMMON_NAME = "vane-busybar"

        private fun parseMode(value: String?): Mode? = when (value?.trim()?.lowercase()) {
            "auto", null, "" -> Mode.AUTO
            "keystore" -> Mode.KEYSTORE
            "off" -> Mode.OFF
            else -> null
        }

        /** Base64url SHA-256 of [certificate]'s SubjectPublicKeyInfo, the value bridges pin. */
        fun pinOf(certificate: X509Certificate): String {
            val hash = MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        }

        private fun contextFor(keyStore: KeyStore, password: CharArray): SSLContext {
            val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                .apply { init(keyStore, password) }
                .keyManagers
            return SSLContext.getInstance("TLS").apply { init(keyManagers, null, null) }
        }

        private fun readPem(file: File): ByteArray {
            val body = file.readLines().filterNot { it.startsWith("-----") }.joinToString("")
            return try {
                Base64.getDecoder().decode(body)
            } catch (e: IllegalArgumentException) {
                throw GeneralSecurityException("$file is not a valid PEM file", e)
            }
        }

        /** Writes [der] as PEM; [private] files get owner-only permissions where the filesystem supports them. */
        private fun writePem(file: File, label: String, der: ByteArray, private: Boolean) {
            val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der)
            val pem = "-----BEGIN $label-----\n$body\n-----END $label-----\n"
            file.delete()
            val path = file.toPath()
            if (private && path.fileSystem.supportedFileAttributeViews().contains("posix")) {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            }
            Files.writeString(path, pem)
        }
    }
}
