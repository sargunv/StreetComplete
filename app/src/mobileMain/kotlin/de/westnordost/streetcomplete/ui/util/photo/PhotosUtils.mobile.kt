package de.westnordost.streetcomplete.ui.util.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitOpenCameraSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

@Composable @ReadOnlyComposable
expect fun createOpenCameraSettings(): FileKitOpenCameraSettings

@Composable
actual fun rememberTakePhotoLauncher(onResult: (PlatformFile?) -> Unit): (PlatformFile) -> Unit {
    val launcher = rememberCameraPickerLauncher(createOpenCameraSettings(), onResult)
    return { file -> launcher.launch(destinationFile = file) }
}
