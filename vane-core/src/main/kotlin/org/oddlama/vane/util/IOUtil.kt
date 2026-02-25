package org.oddlama.vane.util

import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets

object IOUtil {
    @Throws(IOException::class)
    private fun readAll(rd: Reader): String {
        val sb = StringBuilder()
        var cp: Int
        while ((rd.read().also { cp = it }) != -1) {
            sb.append(cp.toChar())
        }
        return sb.toString()
    }

    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun readJsonFromUrl(url: String): JSONObject {
        BufferedReader(
            InputStreamReader(URI(url).toURL().openStream(), StandardCharsets.UTF_8)
        ).use { rd ->
            return JSONObject(readAll(rd))
        }
    }
}
