package org.oddlama.vane.util

private val timeMultiplier: Map<Char, Long> = mapOf(
    's' to 1_000L,        // seconds
    'm' to 60_000L,       // minutes
    'h' to 3_600_000L,    // hours
    'd' to 86_400_000L,   // days
    'w' to 604_800_000L,  // weeks
    'y' to 31_536_000_000L // years
)

@Throws(NumberFormatException::class)
fun parseTime(input: String): Long {
    var ret = 0L
    val parts = input.split("(?<=[^0-9])(?=[0-9])".toRegex()).filter { it.isNotEmpty() }
    for (time in parts) {
        val content = time.split("(?=[^0-9])".toRegex()).filter { it.isNotEmpty() }
        if (content.size != 2) throw NumberFormatException("missing multiplier")
        val numberPart = content[0]
        val unitPart = content[1].replace("and", "").replace("[,+.\\s]+".toRegex(), "")
        if (unitPart.isEmpty()) throw NumberFormatException("missing multiplier")
        val mult = timeMultiplier[unitPart[0]] ?: throw NumberFormatException("unknown multiplier: ${unitPart[0]}")
        ret += numberPart.toLong() * mult
    }
    return ret
}

fun formatTime(millis: Long): String {
    val days    = millis / 86_400_000L
    val hours   = (millis / 3_600_000L) % 24
    val minutes = (millis / 60_000L)    % 60
    val seconds = (millis / 1_000L)     % 60
    return buildString {
        if (days > 0)    append("${days}d")
        if (hours > 0)   append("${hours}h")
        if (minutes > 0) append("${minutes}m")
        if (seconds > 0 || isEmpty()) append("${seconds}s")
    }
}
