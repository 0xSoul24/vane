package org.oddlama.vane.util

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets

object IOUtil {
    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun readJsonFromUrl(url: String): JSONObject =
        URI(url).toURL().openStream().bufferedReader(StandardCharsets.UTF_8).use { rd ->
            JSONObject(rd.readText())
        }
}
