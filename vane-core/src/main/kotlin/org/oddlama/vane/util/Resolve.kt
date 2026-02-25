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
        val url = "https://sessionserver.mojang.com/session/minecraft/profile/$id?unsigned=false"

        val json = readJsonFromUrl(url)
        val skin = Skin()
        val obj = json.getJSONArray("properties").getJSONObject(0)
        skin.texture = obj.getString("value")
        skin.signature = obj.getString("signature")
        return skin
    }

    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun resolveUuid(name: String): UUID {
        val url = "https://api.mojang.com/users/profiles/minecraft/$name"

        val json = readJsonFromUrl(url)
        val idStr = json.getString("id")
        val uuidStr = idStr.replaceFirst(
            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(), "$1-$2-$3-$4-$5"
        )
        return UUID.fromString(uuidStr)
    }

    class Skin {
        @JvmField
        var texture: String? = null
        @JvmField
        var signature: String? = null
    }
}
