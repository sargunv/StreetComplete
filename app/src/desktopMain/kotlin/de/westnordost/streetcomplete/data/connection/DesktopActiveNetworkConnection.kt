package de.westnordost.streetcomplete.data.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.seconds

/** Best-effort connectivity for local development; desktop connections are treated as unmetered. */
class DesktopActiveNetworkConnection : ActiveNetworkConnection {
    override val capabilities = flow {
        while (true) {
            val connected = NetworkInterface.getNetworkInterfaces().toList().any { it.isUp && !it.isLoopback }
            emit(NetworkCapabilities(hasInternet = connected, isMetered = false))
            delay(5.seconds)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
