package de.westnordost.streetcomplete.screens.main.map.layers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.screens.main.map.pinPainter
import de.westnordost.streetcomplete.util.ktx.toPosition
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.IconPitchAlignment
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.DpPadding
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.MultiPoint

/** Displays "selected" pins. Those pins should always be shown on top of pins displayed by
 *  [PinsLayers].
 *
 *  When they are shown, a short springy animation animate them to a larger size.
 *  */
@MaplibreComposable
@Composable
fun SelectedPinsLayer(icon: DrawableResource, pinPositions: Collection<LatLon>) {
    val pinsSize = remember { Animatable(0.5f) }
    LaunchedEffect(pinPositions, icon) {
        pinsSize.snapTo(0.5f)
        pinsSize.animateTo(
            targetValue = 1.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
        )
    }

    val geometry = remember(pinPositions) { MultiPoint(pinPositions.map { it.toPosition() }) }
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(geometry))

    SymbolLayer(
        id = "selected-pins-shadow-layer",
        source = source,
        iconImage = PinGroundShadow.image(),
        iconSize = const(pinsSize.value),
        iconAnchor = const(SymbolAnchor.TopLeft),
        iconOffset = const(PinGroundShadow.offset),
        iconPitchAlignment = const(IconPitchAlignment.Map),
        iconRotationAlignment = const(IconRotationAlignment.Map),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
    )
    SymbolLayer(
        id = "selected-pins-layer",
        source = source,
        iconImage = image(
            pinPainter(painterResource(icon), withCastShadow = false),
            size = DpSize(62.dp, 71.dp)
        ),
        iconSize = const(pinsSize.value),
        iconAnchor = const(SymbolAnchor.Bottom),
        // same as for the pins in PinsLayers
        iconPadding = const(DpPadding(-2.dp, -2.dp, -2.dp, -2.dp)),
    )
}
