package de.westnordost.streetcomplete.data.download

import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Downloads live as long as the desktop application, independently of navigation. */
class DesktopDownloadController(private val downloader: Downloader) : DownloadController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName(Downloader.TAG))
    private val mutex = Mutex()
    private var job: Job? = null

    override fun download(bbox: BoundingBox, isUserInitiated: Boolean) {
        scope.launch {
            mutex.withLock {
                if (isUserInitiated) job?.cancelAndJoin()
                else if (job?.isCompleted == false) return@withLock

                job = scope.launch {
                    try {
                        downloader.download(bbox, isUserInitiated)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Downloader already logs failures and notifies progress listeners.
                    }
                }
            }
        }
    }

    fun close() { scope.cancel() }
}
