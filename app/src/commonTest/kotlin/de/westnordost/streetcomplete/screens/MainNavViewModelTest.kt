package de.westnordost.streetcomplete.screens

import de.westnordost.streetcomplete.data.AllEditTypes
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.overlay.TestOverlayA
import de.westnordost.streetcomplete.data.overlays.OverlayRegistry
import de.westnordost.streetcomplete.data.overlays.SelectedOverlayController
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.presets.EditTypePreset
import de.westnordost.streetcomplete.data.presets.EditTypePresetsController
import de.westnordost.streetcomplete.data.presets.EditTypePresetsDao
import de.westnordost.streetcomplete.data.presets.EditTypePresetsSource
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.quest.TestQuestTypeA
import de.westnordost.streetcomplete.data.urlconfig.UrlConfigController
import de.westnordost.streetcomplete.data.visiblequests.QuestTypeOrderController
import de.westnordost.streetcomplete.data.visiblequests.VisibleEditTypeController
import de.westnordost.streetcomplete.util.ktx.toPosition
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verifyNoMoreCalls
import kotlinx.coroutines.runBlocking
import org.maplibre.compose.camera.CameraPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainNavViewModelTest {
    private val quests = QuestTypeRegistry(listOf(0 to TestQuestTypeA()))
    private val overlays = OverlayRegistry(listOf(0 to TestOverlayA()))
    private val prefs = mock<Preferences>()
    private val presets = mock<EditTypePresetsSource>()
    private val presetsDao = mock<EditTypePresetsDao>()
    private val configController = UrlConfigController(
        quests,
        overlays,
        SelectedOverlayController(prefs, overlays),
        EditTypePresetsController(presetsDao, prefs),
        VisibleEditTypeController(mock(), presets, AllEditTypes(emptyList())),
        QuestTypeOrderController(mock(), presets, quests),
    )
    private val viewModel = MainNavViewModelImpl(configController, presets, prefs)

    @Test fun `geo link requests the map with its coordinates and zoom`() = runBlocking {
        assertTrue(viewModel.setUri("geo:53.0,10.0?z=16"))
        assertEquals(
            CameraPosition(center = LatLon(53.0, 10.0).toPosition(), zoom = 16.0),
            viewModel.geoUri.value,
        )
    }

    @Test fun `geo link without a usable zoom defaults to street level`() = runBlocking {
        for (uri in listOf("geo:53.0,10.0", "geo:53.0,10.0?z=5")) {
            assertTrue(viewModel.setUri(uri))
            assertEquals(18.0, viewModel.geoUri.value?.zoom)
        }
    }

    @Test fun `preset links prepare confirmation without modifying presets`() = runBlocking {
        for (prefix in listOf("streetcomplete://s", "https://streetcomplete.app/s", "http://streetcomplete.app/s")) {
            assertTrue(viewModel.setUri("$prefix?n=My+Preset"))
            assertEquals("My Preset", viewModel.urlConfig.value?.urlConfig?.presetName)
            assertEquals(false, viewModel.urlConfig.value?.alreadyExists)
        }
        verifyNoMoreCalls(presetsDao)
    }

    @Test fun `existing preset is marked for overwrite confirmation`() = runBlocking {
        every { presets.getByName("Existing") } returns EditTypePreset(1, "Existing")

        assertTrue(viewModel.setUri("streetcomplete://s?n=Existing"))
        assertEquals(true, viewModel.urlConfig.value?.alreadyExists)
        verifyNoMoreCalls(presetsDao)
    }

    @Test fun `unrecognized links do not request navigation or change pending requests`() = runBlocking {
        viewModel.setUri("geo:53.0,10.0?z=16")
        viewModel.setUri("streetcomplete://s?n=My+Preset")
        val geo = viewModel.geoUri.value
        val config = viewModel.urlConfig.value

        for (uri in listOf(
            "streetcomplete://oauth?code=123&state=abc",
            "streetcomplete://settings",
            "https://example.org/s?n=Test",
            "streetcomplete://s?n=Test&&q=d",
            "geo:91.0,10.0",
            "not a URI",
        )) {
            assertFalse(viewModel.setUri(uri), uri)
            assertEquals(geo, viewModel.geoUri.value)
            assertEquals(config, viewModel.urlConfig.value)
        }
    }
}
