package com.xenonware.notes.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun NoteTextCard(
    initialTitle: String = "",
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val hazeState = remember { HazeState() }
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    val hazeThinColor = colorScheme.surfaceDim

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = colorScheme.surface
    DisposableEffect(systemUiController, cardBackgroundColor) {
        systemUiController.setStatusBarColor(
            color = cardBackgroundColor
        )
        onDispose {
            systemUiController.setStatusBarColor(
                color = originalStatusBarColor
            )
        }
    }
    val toolbarHeight = 16.dp /*toolbar padding*/ + 64.dp /*toolbar height*/ + 8.dp /*text padding*/
    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + toolbarHeight


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBackgroundColor)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(top = 4.dp)
    ) {
        val topPadding = 68.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier

                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .verticalScroll(scrollState)
                .hazeSource(state = hazeState)

        ) {

            Spacer(modifier = Modifier.height(topPadding))
            val selectiveFontSize = 20.sp
            val noteTextStyle = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = selectiveFontSize)
            )
            //note area
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .focusRequester(focusRequester),
                textStyle = noteTextStyle,
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = "Note",
                                style = noteTextStyle,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                })
            Spacer(modifier = Modifier.height(bottomPadding))

        }
        //topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(100f))
                .background(colorScheme.surfaceDim)
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin(hazeThinColor),
                ), verticalAlignment = Alignment.CenterVertically
        ) {
            //back button
            IconButton(
                onClick = onDismiss, Modifier.padding(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                TextStyle(
                    fontFamily = QuicksandTitleVariable,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface
                )
            )
            //title
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = titleTextStyle,
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = titleTextStyle,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        innerTextField()
                    }
                })
            //more options button
            IconButton(
                onClick = { /*TODO*/ }, Modifier.padding(4.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}
