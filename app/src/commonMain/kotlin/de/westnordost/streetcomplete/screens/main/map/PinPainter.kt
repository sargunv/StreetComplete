package de.westnordost.streetcomplete.screens.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.Painter
import de.westnordost.streetcomplete.resources.*
import org.jetbrains.compose.resources.painterResource

/** Draws a (quest) pin. The [pinShadowPainter] is drawn behind the pin and determines the size.
 *  It is 71 units high and the pin is placed 5 units from its top and right edge, so it can be
 *  either the full shadow (incl. the shadow cast onto the ground, 71 units wide) or just the
 *  halo around the pin (62 units wide), for when the cast shadow is drawn separately on the map. */
class PinPainter(
    private val iconPainter: Painter,
    private val pinPainter: Painter,
    private val pinShadowPainter: Painter,
) : Painter() {
    override val intrinsicSize: Size
        get() = pinShadowPainter.intrinsicSize

    override fun DrawScope.onDraw() {
        val s = size.height / 71f
        val left = size.width - 57f * s

        with(pinShadowPainter) { draw(size) }
        inset(left = left, top = 5f * s, right = 5f * s, bottom = 0f) {
            with(pinPainter) { draw(size) }
            inset(left = 2f * s, top = 2f * s, right = 2f * s, bottom = 16f * s) {
                with(iconPainter) { draw(size) }
            }
        }
    }
}

/** Pin with [iconPainter]. If [withCastShadow] is false, only the halo around the pin is drawn and
 *  the shadow cast onto the ground needs to be drawn separately, flat on the map. */
@Composable
fun pinPainter(iconPainter: Painter, withCastShadow: Boolean = true): Painter {
    val pinPainter = painterResource(Res.drawable.pin)
    val pinShadowPainter = painterResource(
        if (withCastShadow) Res.drawable.pin_shadow else Res.drawable.pin_halo
    )
    return remember(iconPainter, pinPainter, pinShadowPainter) {
        PinPainter(
            iconPainter = iconPainter,
            pinPainter = pinPainter,
            pinShadowPainter = pinShadowPainter
        )
    }
}
