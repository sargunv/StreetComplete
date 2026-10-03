package de.westnordost.streetcomplete

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost

fun main(args: Array<String>) {
    FileKit.init(appId = "StreetComplete-desktop")
    val koin = startKoin { modules(desktopModule, commonModule) }.koin
    koin.get<ApplicationInitializer>().initialize()
    try {
        application(exitProcessOnExit = false) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "StreetComplete — Development",
                state = rememberWindowState(width = 1200.dp, height = 800.dp),
            ) {
                var uri by remember { mutableStateOf(args.firstOrNull()) }
                ProvideMapPresentationHost(rememberAwtComposeMapPresentationHost(window)) {
                    App(uri = uri, onConsumedUri = { uri = null })
                }
            }
        }
    } finally {
        stopKoin()
    }
}
