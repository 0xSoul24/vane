package org.oddlama.vane.util

import org.json.JSONException
import org.oddlama.vane.util.IOUtil.readJsonFromUrl
import java.io.IOException
import java.net.URISyntaxException
import java.util.*

object Resolve {
    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun resolveSkin(id: UUID?): Skin {
        val json = readJsonFromUrl("https://sessionserver.mojang.com/session/minecraft/profile/$id?unsigned=false")
        val obj = json.getJSONArray("properties").getJSONObject(0)
        return Skin(
            texture = obj.getString("value"),
            signature = obj.getString("signature")
        )
    }

    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun resolveUuid(name: String): UUID {
        val json = readJsonFromUrl("https://api.mojang.com/users/profiles/minecraft/$name")
        val uuidStr = json.getString("id")
            .replace(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        return UUID.fromString(uuidStr)
    }

    data class Skin(
        @JvmField var texture: String? = null,
        @JvmField var signature: String? = null
    )
}
