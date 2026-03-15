package org.oddlama.vane.portals.portal

/**
 * Represents the plane in which a portal is aligned.
 *
 * @property x whether the plane spans the x axis.
 * @property y whether the plane spans the y axis.
 * @property z whether the plane spans the z axis.
 */
enum class Plane(private val x: Boolean, private val y: Boolean, private val z: Boolean) {
    XY(true, true, false),
    YZ(false, true, true),
    XZ(true, false, true);

    /** Returns whether this plane spans the x axis. */
    fun x() = x

    /** Returns whether this plane spans the y axis. */
    fun y() = y

    /** Returns whether this plane spans the z axis. */
    fun z() = z
}
