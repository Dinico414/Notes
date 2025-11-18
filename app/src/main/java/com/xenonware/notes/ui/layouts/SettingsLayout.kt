package com.xenonware.notes.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xenonware.notes.presentation.sign_in.SignInState
import com.xenonware.notes.ui.layouts.settings.CoverSettings
import com.xenonware.notes.ui.layouts.settings.DefaultSettings
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.SettingsViewModel

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onNavigateToDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
    state: SignInState,
    onSignInClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                CoverSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                    state = state,
                    onSignInClick = onSignInClick
                )
            }

            LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
                DefaultSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = isLandscape,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                    state = state,
                    onSignInClick = onSignInClick
                )
            }
        }
    }
}
