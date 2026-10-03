package de.westnordost.streetcomplete.screens.user.login

import androidx.compose.runtime.Composable

@Composable
expect fun LoginScreen(viewModel: LoginViewModel, onClickBack: () -> Unit)
