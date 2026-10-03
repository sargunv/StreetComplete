package de.westnordost.streetcomplete.screens.main

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
actual fun rememberEmailAppLauncher(): EmailAppLauncher = object : EmailAppLauncher {
    override fun isAvailable(): Boolean = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)
    override fun compose(email: String, subject: String?, body: String?) {
        val query = listOfNotNull(subject?.let { "subject" to it }, body?.let { "body" to it })
            .joinToString("&") { (key, value) ->
                "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")}"
            }
        Desktop.getDesktop().mail(URI("mailto:$email" + if (query.isEmpty()) "" else "?$query"))
    }
}
