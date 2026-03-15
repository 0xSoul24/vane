package org.oddlama.vane.portals.menu

import java.util.*

/**
 * Stores the portal id associated with an opened portal menu.
 *
 * @property portalId the tagged portal id.
 */
class PortalMenuTag(private val portalId: UUID?) {
    /** Returns the tagged portal id. */
    fun portalId() = portalId
}
