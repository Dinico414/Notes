package com.xenonware.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.xenonware.notes.ui.layouts.NotesListLayout
import com.xenonware.notes.ui.layouts.notes.AudioViewType
import com.xenonware.notes.ui.res.NoteAudioCard
import com.xenonware.notes.ui.theme.ScreenEnvironment
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.NotesViewModel
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val notesViewModel: NotesViewModel by viewModels()
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean =
        false
    private var lastAppliedBlackedOutMode: Boolean = false

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sharedPreferenceManager = SharedPreferenceManager(applicationContext)

        val initialThemePref = sharedPreferenceManager.theme
        // Store the raw setting value for change detection
        val initialCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val initialBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        updateAppCompatDelegateTheme(initialThemePref)

        lastAppliedTheme = initialThemePref
        lastAppliedCoverThemeEnabled = initialCoverThemeEnabledSetting // Store the raw setting
        lastAppliedBlackedOutMode = initialBlackedOutMode

        setContent {
            // val windowSizeClassValue = calculateWindowSizeClass(this) // Not directly used for this logic
            // val currentWidthSizeClass = windowSizeClassValue.widthSizeClass // Not directly used for this logic

            val currentContext = LocalContext.current
            val currentContainerSize = LocalWindowInfo.current.containerSize // Use LocalWindowInfo

            // Determine if cover theme should be ACTUALLY applied based on setting AND screen dimensions
            val applyCoverTheme =
                sharedPreferenceManager.isCoverThemeApplied(currentContainerSize) // Use currentContainerSize

            ScreenEnvironment(
                themePreference = lastAppliedTheme,
                coverTheme = applyCoverTheme, // Use the dynamically calculated value
                blackedOutModeEnabled = lastAppliedBlackedOutMode
            ) { layoutType, isLandscape ->
                XenonApp(
                    viewModel = notesViewModel,
                    layoutType = layoutType,
                    isLandscape = isLandscape,
                    appSize = currentContainerSize, // Pass currentContainerSize
                    onOpenSettings = {
                        val intent = Intent(currentContext, SettingsActivity::class.java)
                        currentContext.startActivity(intent)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val currentThemePref = sharedPreferenceManager.theme
        val currentCoverThemeEnabledSetting =
            sharedPreferenceManager.coverThemeEnabled // Get the raw setting value
        val currentBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        // Check if any of the raw settings have changed
        if (currentThemePref != lastAppliedTheme || currentCoverThemeEnabledSetting != lastAppliedCoverThemeEnabled || // Compare with the stored raw setting
            currentBlackedOutMode != lastAppliedBlackedOutMode
        ) {
            if (currentThemePref != lastAppliedTheme) {
                updateAppCompatDelegateTheme(currentThemePref)
            }

            // Update the last applied raw settings
            lastAppliedTheme = currentThemePref
            lastAppliedCoverThemeEnabled = currentCoverThemeEnabledSetting
            lastAppliedBlackedOutMode = currentBlackedOutMode

            recreate() // Recreate if any setting changed
        }
    }

    private fun updateAppCompatDelegateTheme(themePref: Int) {
        if (themePref >= 0 && themePref < sharedPreferenceManager.themeFlag.size) {
            AppCompatDelegate.setDefaultNightMode(sharedPreferenceManager.themeFlag[themePref])
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

@Composable
fun XenonApp(
    viewModel: NotesViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {
    val context = LocalContext.current

    var showAudioCard by remember { mutableStateOf(false) }
    var currentAudioTitle by remember { mutableStateOf("New Audio Note") }
    var audioPathToLoad by remember { mutableStateOf<String?>(null) } // This holds the permanent path
    var saveTrigger by remember { mutableStateOf(false) }

    // This list will store your "permanently" saved audio notes for demonstration
    val savedAudioNotes = remember { mutableStateListOf<Pair<String, String>>() } // Pair<Title, PermanentFilePath>

    // Function to move the temporary file to permanent storage
    fun saveAudioToFilePermanently(tempFilePath: String, title: String): String? {
        val permanentAudiosDir = File(context.filesDir, "audio_notes")
        if (!permanentAudiosDir.exists()) {
            permanentAudiosDir.mkdirs() // Create the directory if it doesn't exist
        }

        val permanentFileName = "audio_${System.currentTimeMillis()}.mp3"
        val permanentFile = File(permanentAudiosDir, permanentFileName)

        return try {
            val tempFile = File(tempFilePath)
            if (tempFile.exists()) {
                tempFile.copyTo(permanentFile, overwrite = true)
                tempFile.delete() // Delete the temporary file after successful copy
                println("Parent: Audio successfully saved permanently to: ${permanentFile.absolutePath}")
                permanentFile.absolutePath
            } else {
                println("Parent: Error: Temporary audio file not found at $tempFilePath")
                null
            }
        } catch (e: IOException) {
            println("Parent: Error copying audio file: ${e.message}")
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Your existing NotesListLayout (if you want to keep it visible under the demo)
        // If you want the demo to fully take over the screen, remove or comment this out
        NotesListLayout(
            viewModel = viewModel,
            isLandscape = isLandscape,
            layoutType = layoutType,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.weight(1f), // Give it some weight if present
            appSize = appSize
        )

        // --- DEMONSTRATION UI FOR AUDIO RECORDING/PLAYBACK ---
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Audio Demo Controls", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            // Button to open NoteAudioCard for a new recording
            Button(onClick = {
                showAudioCard = true
                audioPathToLoad = null // Set to null to indicate a new recording session
                currentAudioTitle = "New Audio Note"
            }) {
                Text("Record New Audio")
            }

            Spacer(Modifier.height(16.dp))

            Text("Saved Audio Notes:", style = MaterialTheme.typography.titleMedium)
            // List and buttons to play saved audio notes
            if (savedAudioNotes.isEmpty()) {
                Text("No audio notes saved yet.")
            } else {
                savedAudioNotes.forEach { (title, path) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(title, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            showAudioCard = true
                            audioPathToLoad = path // Set the permanent path to load
                            currentAudioTitle = title
                        }) {
                            Text("Play/Edit")
                        }
                    }
                }
            }
        }
    }

    // This is where NoteAudioCard is shown when 'showAudioCard' is true
    if (showAudioCard) {
        NoteAudioCard(
            audioTitle = currentAudioTitle,
            onAudioTitleChange = { currentAudioTitle = it },
            onDismiss = {
                showAudioCard = false
                audioPathToLoad = null // Reset for next session
                currentAudioTitle = "New Audio Note"
            },
            onSave = { title, tempFilePath ->
                val newPermanentPath = saveAudioToFilePermanently(tempFilePath, title)
                if (newPermanentPath != null) {
                    savedAudioNotes.add(title to newPermanentPath) // Add to our demo list
                    audioPathToLoad = newPermanentPath // Update to reflect the newly saved permanent file
                    currentAudioTitle = title
                }
                saveTrigger = false // Consume the trigger inside onSave after processing
            },
            cardBackgroundColor = MaterialTheme.colorScheme.surfaceContainer,
            toolbarHeight = 56.dp, // Example value
            saveTrigger = saveTrigger,
            onSaveTriggerConsumed = { saveTrigger = false },
            selectedAudioViewType = AudioViewType.Waveform,
            initialAudioFilePath = audioPathToLoad // THIS IS KEY: Pass the permanent path here
        )
    }
}
