package de.westnordost.streetcomplete.screens.main.map.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.westnordost.streetcomplete.resources.Res
import de.westnordost.streetcomplete.resources.pin_circle
import de.westnordost.streetcomplete.screens.main.map.MapImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.expressions.dsl.all
import org.maplibre.compose.expressions.dsl.any
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToNumber
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.div
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.gt
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.log2
import org.maplibre.compose.expressions.dsl.lte
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.dsl.sp
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.textOffset
import org.maplibre.compose.expressions.dsl.zoom
import org.maplibre.compose.expressions.value.IconPitchAlignment
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.value.SymbolZOrder
import org.maplibre.compose.expressions.value.TranslateAnchor
import org.maplibre.compose.interaction.ClickEvent
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.LocalMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point

/** Display pins on the map, e.g. quest pins or pins for recent edits. Clicking a cluster of pins
 *  reports its expansion zoom via [onZoomToCluster]. */
@MaplibreComposable
@Composable
fun PinsLayers(
    pins: Collection<Pin>,
    mapImages: MapImages,
    onClickPin: (properties: JsonObject) -> ClickResult,
    onZoomToCluster: (targetZoom: Double) -> Unit,
) {
    val mapState = checkNotNull(LocalMapState.current)
    val coroutineScope = rememberCoroutineScope()

    val pinIcons = remember(pins) { pins.mapTo(LinkedHashSet()) { it.icon } }
    val pinPainters = pinIcons.associateWith { painterResource(it) }
    val features by produceState<List<Feature<Point, JsonObject>>>(
        emptyList(), pins, mapImages, pinPainters
    ) {
        // images must be there before the features: MapLibre lays out a symbol whose text image is
        // missing without it and (unlike for icon images) does not lay it out again once added
        mapImages.addPins(pinPainters)
        value = withContext(Dispatchers.Default) {
            pins.sortedBy { it.order }.map { it.toGeoJsonFeature() }
        }
    }
    val source = rememberGeoJsonSource(GeoJsonData.Features(FeatureCollection(features))) {
        cluster = true
        clusterMaxZoom = CLUSTER_MAX_ZOOM
        clusterRadius = 55
    }
    // the pins have their own unclustered source because a clustered source does not retain the
    // order of the features, which determines which pins are placed first, see below
    val pinsSource = rememberGeoJsonSource(GeoJsonData.Features(FeatureCollection(features)))

    val currentOnZoomToCluster by rememberUpdatedState(onZoomToCluster)
    val currentOnClickPin by rememberUpdatedState(onClickPin)

    fun ClickEvent.onClickClusterFeature(features: List<Feature<Geometry, JsonObject?>>): ClickResult {
        val feature = features.firstOrNull() ?: return ClickResult.Pass
        val handle = mapState.style.sources[source] ?: return ClickResult.Pass
        coroutineScope.launch {
            val zoom = handle.getClusterExpansionZoom(feature) ?: return@launch
            currentOnZoomToCluster(zoom)
        }
        return ClickResult.Consume
    }

    fun ClickEvent.onClick(features: List<Feature<Geometry, JsonObject?>>): ClickResult {
        val properties = features.firstOrNull()?.properties ?: return ClickResult.Pass
        return currentOnClickPin(properties)
    }

    SymbolLayer(
        id = "pin-cluster-layer",
        source = source,
        minZoom = CLUSTER_MIN_ZOOM.toFloat(),
        filter = all(
            zoom().lte(const(CLUSTER_MAX_ZOOM)),
            feature["point_count"].convertToNumber().gt(const(1)),
        ),
        iconImage = image(painterResource(Res.drawable.pin_circle)),
        iconSize = const(0.5f) + (log2(feature["point_count"].convertToNumber()) / const(10f)),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        textField = feature["point_count"].convertToString(),
        textSize = (const(15f) + log2(feature["point_count"].convertToNumber()) / const(1.5f)).sp,
        textFont = const(listOf("Roboto Regular")),
        textOffset = textOffset(0.em, 0.1.em),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true),
        onClick = ClickEvent::onClickClusterFeature,
    )
    CircleLayer(
        id = "pin-dot-layer",
        source = source,
        minZoom = CLUSTER_MAX_ZOOM.toFloat(),
        filter = any(
            zoom().gt(const(CLUSTER_MAX_ZOOM)),
            feature["point_count"].convertToNumber(const(1)).lte(const(1)),
        ),
        color = const(Color.White),
        radius = const(5.dp),
        strokeColor = const(Color(0xffaaaaaa)),
        strokeWidth = const(1.dp),
        translate = offset(0.dp, -12.dp), // so that it hides behind the pin
        translateAnchor = const(TranslateAnchor.Viewport),
    )
    // The pin is drawn as an image in the text, so that it stands upright, while its shadow is
    // drawn as the icon, lying flat on the map. Icon and text of a symbol are shown or hidden
    // together on collision, so a shadow is never displayed without its pin.
    // Within a tile, all icons are drawn before all text, so all shadows are below all pins. This
    // is not the case when a symbol sort key is used (icons and text are then drawn interleaved
    // per sort key), so the priority of the pins is determined by the order of the features.
    SymbolLayer(
        id = "pins-layer",
        source = pinsSource,
        filter = zoom().gt(const(CLUSTER_MAX_ZOOM)),
        zOrder = const(SymbolZOrder.Source),
        iconImage = PinGroundShadow.image(),
        iconAnchor = const(SymbolAnchor.TopLeft),
        iconOffset = const(PinGroundShadow.offset),
        iconPitchAlignment = const(IconPitchAlignment.Map),
        iconRotationAlignment = const(IconRotationAlignment.Map),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        textField = format(span(image(feature["icon-image"].convertToString()))),
        textAnchor = const(SymbolAnchor.Bottom),
        // the image is aligned to the bottom of a line, so a line height larger than the default
        // text size would leave a gap between the pin's tip and its position
        textLineHeight = const(1.em),
        textPadding = const((-2).dp),
        textAllowOverlap = const(false),
        textIgnorePlacement = const(false),
        onClick = ClickEvent::onClick,
    )
}

private const val CLUSTER_MIN_ZOOM = 13
private const val CLUSTER_MAX_ZOOM = 14
