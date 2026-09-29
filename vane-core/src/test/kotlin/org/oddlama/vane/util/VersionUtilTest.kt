package org.oddlama.vane.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionUtilTest {
    @Test
    fun `orders by major, minor and patch numerically`() {
        assertTrue(VersionUtil.isNewer("v1.23.0", "v1.22.0"))
        assertTrue(VersionUtil.isNewer("v1.22.10", "v1.22.9"))
        assertTrue(VersionUtil.isNewer("v2.0.0", "v1.99.99"))
        assertFalse(VersionUtil.isNewer("v1.22.0", "v1.22.0"))
    }

    @Test
    fun `a stable release is not newer than a later pre-release`() {
        // A beta user must not be told to "update" to the previous stable release.
        assertFalse(VersionUtil.isNewer("v1.22.0", "v1.23.0-beta.1"))
        assertTrue(VersionUtil.isNewer("v1.23.0", "v1.23.0-beta.1"))
    }

    @Test
    fun `orders pre-release identifiers by semver precedence`() {
        assertTrue(VersionUtil.isNewer("v1.23.0-beta.2", "v1.23.0-beta.1"))
        assertTrue(VersionUtil.isNewer("v1.23.0-beta.10", "v1.23.0-beta.9"))
        assertTrue(VersionUtil.isNewer("v1.23.0-beta", "v1.23.0-alpha.5"))
        assertTrue(VersionUtil.isNewer("v1.23.0-beta.1", "v1.23.0-beta"))
        assertEquals(0, VersionUtil.compare("1.23.0-beta.1", "v1.23.0-beta.1"))
    }

    @Test
    fun `unparsable versions are never reported as newer`() {
        assertNull(VersionUtil.compare("latest", "v1.22.0"))
        assertFalse(VersionUtil.isNewer("latest", "v1.22.0"))
    }
}
