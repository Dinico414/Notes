package com.xenonware.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xenonware.notes.data.SharedPreferenceManager
import com.xenonware.notes.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.notes.presentation.sign_in.SignInViewModel
import com.xenonware.notes.ui.layouts.SettingsLayout
import com.xenonware.notes.ui.theme.ScreenEnvironment
import com.xenonware.notes.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch


object SettingsDestinations {
    const val MAIN_SETTINGS_ROUTE = "main_settings"
}

class SettingsActivity : ComponentActivity() {

    private val sharedPreferenceManager by lazy { SharedPreferenceManager(application) }  // Add if missing

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var signInViewModel: SignInViewModel

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.SettingsViewModelFactory(application)
        )[SettingsViewModel::class.java]

        signInViewModel = ViewModelProvider(
            this,
            SignInViewModel.SignInViewModelFactory(application)
        )[SignInViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val currentContainerSize = LocalWindowInfo.current.containerSize
            val activeNightMode by settingsViewModel.activeNightModeFlag.collectAsState()
            LaunchedEffect(activeNightMode) {
                AppCompatDelegate.setDefaultNightMode(activeNightMode)
            }

            val persistedAppThemeIndex by settingsViewModel.persistedThemeIndex.collectAsState()
            val blackedOutEnabled by settingsViewModel.blackedOutModeEnabled.collectAsState()
            val coverThemeEnabled by settingsViewModel.enableCoverTheme.collectAsState()
            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = remember(containerSize, coverThemeEnabled) {
                settingsViewModel.applyCoverTheme(containerSize)
            }

            ScreenEnvironment(
                persistedAppThemeIndex, applyCoverTheme, blackedOutEnabled
            ) { layoutType, isLandscape ->

                val context = LocalContext.current
                val state by signInViewModel.state.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = SettingsDestinations.MAIN_SETTINGS_ROUTE
                ) {
                    composable(SettingsDestinations.MAIN_SETTINGS_ROUTE) {
                        SettingsLayout(
                            onNavigateBack = { finish() },
                            viewModel = settingsViewModel,
                            isLandscape = isLandscape,
                            layoutType = layoutType,
                            onNavigateToDeveloperOptions = {
                                val intent = Intent(context, DevSettingsActivity::class.java)
                                context.startActivity(intent)
                            },
                            state = state,
                            googleAuthUiClient = googleAuthUiClient,
                            onSignInClick = {
                                lifecycleScope.launch {
                                    val signInResult = googleAuthUiClient.signIn(context)
                                    signInViewModel.onSignInResult(signInResult)
                                }
                            },
                            onSignOutClick = {
                                settingsViewModel.onSignOutClicked()
                            },
                            onConfirmSignOut = {
                                lifecycleScope.launch {
                                    googleAuthUiClient.signOut(context)
                                    
                                    val offlineNotes = sharedPreferenceManager.notesItems.filter { it.isOffline }
                                    sharedPreferenceManager.notesItems = offlineNotes
                                    
                                    sharedPreferenceManager.isUserLoggedIn = false
                                    settingsViewModel.dismissSignOutDialog()
                                    signInViewModel.resetState()
                                    
                                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            },
                            appSize = currentContainerSize
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.updateCurrentLanguage()
        settingsViewModel.refreshDeveloperModeState()
        lifecycleScope.launch {
            val user = googleAuthUiClient.getSignedInUser()
            val isSignedIn = user != null
            sharedPreferenceManager.isUserLoggedIn = isSignedIn
            signInViewModel.updateSignInState(isSignedIn)
        }
    }
}
