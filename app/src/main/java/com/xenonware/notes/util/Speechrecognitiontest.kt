package com.xenonware.notes.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * DIAGNOSTIC TEST SCREEN
 * Add this temporarily to your app to test if speech recognition works
 *
 * Usage:
 * In your navigation/screen, add:
 * SpeechRecognitionTest()
 */
@Composable
fun SpeechRecognitionTest() {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("Ready to test...\n") }

    val speechRecognitionManager = remember {
        SpeechRecognitionManager(context).apply {
            onTranscriptUpdate = { segments ->
                log += "\n✓ UPDATE: ${segments.size} segments\n"
                segments.forEach { seg ->
                    log += "  - [${seg.timestampMillis}ms] ${seg.text}\n"
                }
            }
            onError = { error ->
                log += "\n✗ ERROR: $error\n"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            log += "✓ Permission granted\n"
            isListening = true
            log += "Starting speech recognition...\n"
            speechRecognitionManager.startListening(System.currentTimeMillis())
        } else {
            log += "✗ Permission denied!\n"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognitionManager.dispose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Speech Recognition Test",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Status indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusCard(
                title = "Available",
                value = speechRecognitionManager.isAvailable().toString(),
                isGood = speechRecognitionManager.isAvailable()
            )

            StatusCard(
                title = "Listening",
                value = isListening.toString(),
                isGood = isListening
            )

            StatusCard(
                title = "Transcribing",
                value = speechRecognitionManager.isTranscribing.toString(),
                isGood = speechRecognitionManager.isTranscribing
            )
        }

        Spacer(Modifier.height(16.dp))

        // Partial text display
        if (speechRecognitionManager.currentPartialText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Speaking: ${speechRecognitionManager.currentPartialText}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Error message
        speechRecognitionManager.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        isListening = true
                        log += "\n▶ Starting...\n"
                        speechRecognitionManager.startListening(System.currentTimeMillis())
                    } else {
                        log += "Requesting permission...\n"
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !isListening
            ) {
                Icon(Icons.Rounded.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text("Start")
            }

            Button(
                onClick = {
                    isListening = false
                    log += "\n■ Stopping...\n"
                    speechRecognitionManager.stopListening()
                },
                enabled = isListening,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Rounded.Stop, null)
                Spacer(Modifier.width(8.dp))
                Text("Stop")
            }

            Button(
                onClick = {
                    log = "Log cleared.\n"
                    speechRecognitionManager.clearTranscript()
                }
            ) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Instructions:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "1. Click 'Start'\n" +
                    "2. Wait for 'Listening for audio...'\n" +
                    "3. Speak clearly into microphone\n" +
                    "4. Watch for partial results\n" +
                    "5. Check log below",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Log display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = "Log:",
                    style = MaterialTheme.typography.titleSmall
                )
                Divider(Modifier.padding(vertical = 4.dp))
                rememberScrollState().let { scrollState ->
                    LaunchedEffect(log) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )

                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    isGood: Boolean
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGood)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isGood)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}