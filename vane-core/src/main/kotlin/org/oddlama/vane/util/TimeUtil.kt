package org.oddlama.vane.util

object TimeUtil {
    private val timeMultiplier: MutableMap<Char, Long>

    init {
        val mult: MutableMap<Char, Long> = HashMap()
        mult['s'] = 1000L // seconds
        mult['m'] = 60000L // minutes
        mult['h'] = 3600000L // hours
        mult['d'] = 86400000L // days
        mult['w'] = 604800000L // weeks
        mult['y'] = 31536000000L // years
        timeMultiplier = mult
    }

    @Throws(NumberFormatException::class)
    fun parseTime(input: String): Long {
        var ret: Long = 0

        val parts = input.split("(?<=[^0-9])(?=[0-9])".toRegex()).filter { it.isNotEmpty() }

        for (time in parts) {
            val content: Array<String> = time.split("(?=[^0-9])".toRegex()).filter { it.isNotEmpty() }.toTypedArray()

            if (content.size != 2) {
                throw NumberFormatException("missing multiplier")
            }

            val numberPart = content[0]
            val unitPart = content[1].replace("and", "").replace("[,+.\\s]+".toRegex(), "")
            if (unitPart.isEmpty()) throw NumberFormatException("missing multiplier")

            val keyChar = unitPart[0]
            val mult: Long = timeMultiplier[keyChar]
                ?: throw NumberFormatException("unknown multiplier: $keyChar")

            ret += numberPart.toLong() * mult
        }

        return ret
    }

    @JvmStatic
    fun formatTime(millis: Long): String {
        var ret = ""

        val days = millis / 86400000L
        val hours = (millis / 3600000L) % 24
        val minutes = (millis / 60000L) % 60
        val seconds = (millis / 1000L) % 60

        if (days > 0) {
            ret += days.toString() + "d"
        }

        if (hours > 0) {
            ret += hours.toString() + "h"
        }

        if (minutes > 0) {
            ret += minutes.toString() + "m"
        }

        if (seconds > 0 || ret.isEmpty()) {
            ret += seconds.toString() + "s"
        }

        return ret
    }
}
