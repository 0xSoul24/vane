package org.oddlama.vane.proxycore

import java.util.*

object Util {
    fun addUuid(uuid: UUID, i: Long): UUID {
        var msb = uuid.mostSignificantBits
        var lsb = uuid.leastSignificantBits

        lsb += i
        if (lsb < uuid.leastSignificantBits) {
            ++msb
        }

        return UUID(msb, lsb)
    }
}
