package org.oddlama.vane.portals.portal

enum class Plane(private val x: Boolean, private val y: Boolean, private val z: Boolean) {
    XY(true, true, false),
    YZ(false, true, true),
    XZ(true, false, true);

    fun x(): Boolean {
        return x
    }

    fun y(): Boolean {
        return y
    }

    fun z(): Boolean {
        return z
    }
}
