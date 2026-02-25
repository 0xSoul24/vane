package org.oddlama.vane.core.resourcepack

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackGenerator {
    // Use Kotlin mutable maps and non-nullable types for clarity and correctness
    private val translations: MutableMap<String, MutableMap<String, JSONObject>> = mutableMapOf()
    private val files: MutableMap<String, ByteArray> = mutableMapOf()

    fun translations(namespace: String, langCode: String): JSONObject {
        // Use getOrPut to create inner maps and JSON objects idiomatically
        val ns = translations.getOrPut(namespace) { mutableMapOf() }
        return ns.getOrPut(langCode) { JSONObject() }
    }

    @Throws(IOException::class)
    private fun writeTranslations(zip: ZipOutputStream) {
        for ((namespace, map) in translations) {
            for ((langCode, langMap) in map) {
                zip.putNextEntry(ZipEntry("assets/$namespace/lang/$langCode.json"))
                zip.write(langMap.toString().toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    @Throws(IOException::class)
    fun addFile(path: String, stream: InputStream) {
        files[path] = stream.readAllBytes()
    }

    @Throws(IOException::class)
    fun write(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            writeTranslations(zip)
            // Add all files
            for ((path, content) in files) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }
}
