package org.oddlama.vane.busybar

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

class HeadsTest {
    private val face = 0xFF_C0_80_40.toInt()
    private val hat = 0xFF_20_40_E0.toInt()

    /** A skin whose face is [face] and whose hat layer is [hat] in one corner, transparent elsewhere. */
    private fun skin(height: Int, fullHat: Boolean): ByteArray {
        val image = BufferedImage(64, height, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 8) for (y in 0 until 8) {
            image.setRGB(8 + x, 8 + y, face)
            if (fullHat || (x == 0 && y == 0)) image.setRGB(40 + x, 8 + y, hat)
        }
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    private fun render(skin: ByteArray): BufferedImage =
        ImageIO.read(ByteArrayInputStream(Heads.renderFace(skin)))

    @Test
    fun `draws the hat layer over the face`() {
        val out = render(skin(64, fullHat = false))
        assertEquals(8, out.width)
        assertEquals(hat, out.getRGB(0, 0))
        assertEquals(face, out.getRGB(4, 4))
    }

    @Test
    fun `keeps an opaque hat on current skins`() {
        assertEquals(hat, render(skin(64, fullHat = true)).getRGB(4, 4))
    }

    @Test
    fun `ignores an opaque hat on legacy 64x32 skins`() {
        assertEquals(face, render(skin(32, fullHat = true)).getRGB(4, 4))
    }
}
