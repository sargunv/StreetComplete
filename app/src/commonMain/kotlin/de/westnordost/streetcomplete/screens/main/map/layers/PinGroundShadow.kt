package de.westnordost.streetcomplete.screens.main.map.layers

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.pin_ground_shadow
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.ImageValue
import org.jetbrains.compose.resources.painterResource

/** The shadow that a pin casts onto the ground. It is to be drawn as an icon lying flat on the
 *  map (pitch and rotation alignment "map"), so that it tilts and rotates together with the map,
 *  i.e. as if the sun was always shining from the same direction (from the south-southeast).
 *
 *  It looks the same as the shadow baked into the pin image when the map is not rotated or
 *  tilted. */
object PinGroundShadow {
    @Composable
    fun image(): Expression<ImageValue> =
        image(painterResource(Res.drawable.pin_ground_shadow), size = DpSize(66.dp, 58.dp))

    /** Offset to use with anchor top-left so that the tip of the shadow is at the pin's position */
    val offset = DpOffset((-46).dp, (-54).dp)
}
