package de.westnordost.streetcomplete

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.data.Cleaner
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.DatabaseImpl
import de.westnordost.streetcomplete.data.PeriodicCleaner
import de.westnordost.streetcomplete.data.StreetCompleteDatabaseConfigurator
import de.westnordost.streetcomplete.data.connection.ActiveNetworkConnection
import de.westnordost.streetcomplete.data.connection.DesktopActiveNetworkConnection
import de.westnordost.streetcomplete.data.download.DesktopDownloadController
import de.westnordost.streetcomplete.data.download.DownloadController
import de.westnordost.streetcomplete.data.initialize
import de.westnordost.streetcomplete.data.maptiles.MapLibreMapTilesDownloader
import de.westnordost.streetcomplete.data.maptiles.MapTilesDownloader
import de.westnordost.streetcomplete.data.upload.UploadController
import de.westnordost.streetcomplete.screens.about.AppStoreInfo
import de.westnordost.streetcomplete.ui.util.measure.ArSupportChecker
import de.westnordost.streetcomplete.util.error_reporting.CrashReportHolder
import de.westnordost.streetcomplete.util.sound.SoundEffectPlayer
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.maplibre.compose.location.LocationProvider
import org.maplibre.compose.location.createDefaultLocationProvider
import org.maplibre.compose.map.MapRuntime
import org.maplibre.compose.map.MapRuntimeOptions
import org.maplibre.compose.map.createMapRuntime
import java.awt.GraphicsEnvironment
import java.util.prefs.Preferences

/** Services for the local development launcher; metadata is read directly from the checkout. */
val desktopModule = module {
    val resourcesDir = Path(requireNotNull(System.getProperty("streetcomplete.resources")))

    single<de.westnordost.countryboundaries.CountryBoundaries> {
        SystemFileSystem.source(Path(resourcesDir, "boundaries.ser")).buffered().use {
            de.westnordost.countryboundaries.CountryBoundaries.deserializeFrom(it)
        }
    }
    single<FeatureDictionary> {
        FeatureDictionary.create(
            fileSystem = SystemFileSystem,
            presetsBasePath = Path(resourcesDir, "osmfeatures/default").toString(),
            brandPresetsBasePath = Path(resourcesDir, "osmfeatures/brands").toString(),
        )
    }
    single<Database> {
        val databaseFile = Path(FileKit.filesDir.path, ApplicationConstants.DATABASE_NAME)
        DatabaseImpl(BundledSQLiteDriver().open(databaseFile.toString())).apply {
            initialize(StreetCompleteDatabaseConfigurator)
        }
    } onClose { it?.close() }
    factory(named("AvatarsCacheDirectory")) {
        Path(FileKit.cacheDir.path, ApplicationConstants.AVATARS_CACHE_DIRECTORY)
    }
    single<ObservableSettings> {
        PreferencesSettings(Preferences.userRoot().node("de/westnordost/streetcomplete/desktop"))
    }
    single<CrashReportHolder> { object : CrashReportHolder {
        override fun takeCrashReport(): String? = null
    } }
    single<AppStoreInfo> { object : AppStoreInfo {
        override fun getRatingUri(): String? = null
        override fun disallowsInAppDonationLinks(): Boolean = false
    } }
    factory<ArSupportChecker> { object : ArSupportChecker {
        override fun invoke(): Boolean = false
    } }
    single<SoundEffectPlayer> { object : SoundEffectPlayer {
        override fun play(resourcePath: String) {}
    } }

    single<LocationProvider> { createDefaultLocationProvider() } onClose { it?.close() }
    single<ActiveNetworkConnection> { DesktopActiveNetworkConnection() }
    single<MapRuntime> {
        createMapRuntime(MapRuntimeOptions(cacheFile = Path(FileKit.filesDir.path, "maplibre-cache.db")))
    } onClose { it?.close() }
    factory<MapTilesDownloader> {
        val density = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX.toFloat()
        MapLibreMapTilesDownloader(get<MapRuntime>().offlineManager, density)
    }
    single<DownloadController> { DesktopDownloadController(get()) } onClose {
        (it as? DesktopDownloadController)?.close()
    }
    // Login is disabled; edits remain in the local database for inspecting quest behavior.
    single<UploadController> { object : UploadController {
        override fun upload(isUserInitiated: Boolean) {}
    } }
    single<PeriodicCleaner> { object : PeriodicCleaner {
        override fun enqueue() { get<Cleaner>().cleanOld() }
    } }
}
