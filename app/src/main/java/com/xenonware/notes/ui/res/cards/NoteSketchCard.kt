package com.xenonware.notes.ui.res.cards

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.ui.theme.noteBlueDark
import com.xenonware.notes.ui.theme.noteBlueLight
import com.xenonware.notes.ui.theme.noteGreenDark
import com.xenonware.notes.ui.theme.noteGreenLight
import com.xenonware.notes.ui.theme.noteOrangeDark
import com.xenonware.notes.ui.theme.noteOrangeLight
import com.xenonware.notes.ui.theme.notePurpleDark
import com.xenonware.notes.ui.theme.notePurpleLight
import com.xenonware.notes.ui.theme.noteRedDark
import com.xenonware.notes.ui.theme.noteRedLight
import com.xenonware.notes.ui.theme.noteTurquoiseDark
import com.xenonware.notes.ui.theme.noteTurquoiseLight
import com.xenonware.notes.ui.theme.noteYellowDark
import com.xenonware.notes.ui.theme.noteYellowLight
import com.xenonware.notes.viewmodel.NotesViewModel
import com.xenonware.notes.viewmodel.PrebuiltPathData
import com.xenonware.notes.viewmodel.classes.NotesItems

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteSketchCard(
    item: NotesItems,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onSelectItem: () -> Unit,
    onEditItem: (NotesItems) -> Unit,
    notesViewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    isBlackThemeActive: Boolean = false,
    isCoverModeActive: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val colorToThemeName = remember {
        mapOf(
            noteRedLight.value to "Red",
            noteRedDark.value to "Red",
            noteOrangeLight.value to "Orange",
            noteOrangeDark.value to "Orange",
            noteYellowLight.value to "Yellow",
            noteYellowDark.value to "Yellow",
            noteGreenLight.value to "Green",
            noteGreenDark.value to "Green",
            noteTurquoiseLight.value to "Turquoise",
            noteTurquoiseDark.value to "Turquoise",
            noteBlueLight.value to "Blue",
            noteBlueDark.value to "Blue",
            notePurpleLight.value to "Purple",
            notePurpleDark.value to "Purple"
        )
    }
    val selectedTheme = item.color?.let { colorToThemeName[it.toULong()] } ?: "Default"

    XenonTheme(
        darkTheme = isDarkTheme,
        useRedTheme = selectedTheme == "Red",
        useOrangeTheme = selectedTheme == "Orange",
        useYellowTheme = selectedTheme == "Yellow",
        useGreenTheme = selectedTheme == "Green",
        useTurquoiseTheme = selectedTheme == "Turquoise",
        useBlueTheme = selectedTheme == "Blue",
        usePurpleTheme = selectedTheme == "Purple",
        dynamicColor = selectedTheme == "Default"
    ) {
        val currentColorScheme = colorScheme
        val currentExtendedColorScheme = extendedMaterialColorScheme

        val themeDrawColors = remember(currentColorScheme, currentExtendedColorScheme, isDarkTheme) {
            listOf(
                currentColorScheme.onSurface,
                currentExtendedColorScheme.drawRed,
                currentExtendedColorScheme.drawOrange,
                currentExtendedColorScheme.drawYellow,
                currentExtendedColorScheme.drawGreen,
                currentExtendedColorScheme.drawTurquoise,
                currentExtendedColorScheme.drawBlue,
                currentExtendedColorScheme.drawPurple
            )
        }

        val borderColor by animateColorAsState(
            targetValue = if (isSelected) colorScheme.primary else Color.Transparent,
            label = "Border Color Animation"
        )

        val backgroundColor =
            if (selectedTheme == "Default") colorScheme.surfaceBright else colorScheme.inversePrimary

        val backgroundColorState by animateColorAsState(
            targetValue = if (selectedTheme == "Default") colorScheme.surfaceContainer else colorScheme.secondaryContainer,
            animationSpec = tween(durationMillis = 500), label = "backgroundColorState"
        )

        val canvasBackgroundColor = if (isCoverModeActive || isBlackThemeActive) Color.Black else backgroundColorState

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MediumCornerRadius))
                .background(backgroundColor)
                .border(
                    width = 2.dp, color = borderColor, shape = RoundedCornerShape(MediumCornerRadius)
                )
                .then(
                    Modifier.border(
                        width = 0.5.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.075f),
                        shape = RoundedCornerShape(MediumCornerRadius)
                    )
                )
                .combinedClickable(
                    onClick = {
                        if (isSelectionModeActive) {
                            onSelectItem()
                        } else {
                            onEditItem(item)
                        }
                    }, onLongClick = onSelectItem
                )
        ) {
            Column(
                modifier = Modifier.padding(LargestPadding)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = QuicksandTitleVariable,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurface
                )

                val prebuiltPaths = notesViewModel.getPrebuiltSketchPaths(item)

                // Only remap colors — Path objects are already built in the cache
                val memoizedPaths = remember(prebuiltPaths, themeDrawColors) {
                    prebuiltPaths.map { p ->
                        val color = if (p.colorIndex in themeDrawColors.indices)
                            themeDrawColors[p.colorIndex] else p.color
                        val fill = if (p.isShape && p.fillColor != Color.Transparent &&
                            p.fillColorIndex in themeDrawColors.indices)
                            themeDrawColors[p.fillColorIndex] else p.fillColor
                        ResolvedPathData(p, color, fill)
                    }
                }

                if (memoizedPaths.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))

                    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

                    val heightRatio = when (maxLines) {
                        3 -> 0.25f
                        9 -> 0.5f
                        else -> 1f
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                    ) {
                        val scaleFactor = maxWidth.value / screenWidthDp.value
                        val targetHeightDp = screenHeightDp * heightRatio * scaleFactor

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(canvasBackgroundColor)
                                .border(
                                    width = 1.dp,
                                    color = colorScheme.onSurface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .height(targetHeightDp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(screenWidthDp, screenHeightDp)
                                    .graphicsLayer {
                                        scaleX = scaleFactor
                                        scaleY = scaleFactor
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        val offsetY = (screenHeightDp.toPx() * (1f - heightRatio)) / 2f
                                        translationY = -offsetY * scaleFactor
                                    }
                            ) {
                                for (resolved in memoizedPaths) {
                                    val src = resolved.source

                                    if (src.isShape && resolved.fillColor != Color.Transparent) {
                                        drawPath(path = src.path, color = resolved.fillColor, style = Fill)
                                    }

                                    if (src.pointsCount < 2) {
                                        if (src.pointsCount == 1) {
                                            drawCircle(
                                                color = resolved.color,
                                                radius = src.thickness / 2,
                                                center = src.firstPoint
                                            )
                                        }
                                        continue
                                    }

                                    drawPath(
                                        path = src.path,
                                        color = resolved.color,
                                        style = Stroke(
                                            width = src.thickness,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelectionModeActive,
                modifier = Modifier.align(Alignment.TopStart),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .background(backgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(isSelected) { selected ->
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, "Selected", tint = colorScheme.primary)
                        } else {
                            Box(
                                Modifier.padding(2.dp).size(20.dp)
                                    .border(2.dp, colorScheme.onSurface.copy(0.6f), CircleShape)
                            )
                        }
                    }
                }
            }

            //Sync Icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
            ) {
                val isLocalOnly = item.isOffline
                val isSyncing = notesViewModel.isNoteBeingSynced(item.id)

                when {
                    isSyncing -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "spin")

                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spinAngle"
                        )

                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Syncing",
                            tint = colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(angle)
                        )
                    }
                    isLocalOnly -> {
                        Icon(Icons.Rounded.CloudOff, "Local only", tint = colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    else -> {
                        Icon(Icons.Rounded.CloudDone, "Synced", tint = colorScheme.primary)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 6.dp)
                    .size(26.dp), contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(colorScheme.onSurface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Sketch",
                        tint = backgroundColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lightweight wrapper that pairs a pre-built [PrebuiltPathData] (which owns the
 * expensive [Path] object) with theme-resolved colors. Only colors change per
 * recomposition; the Path itself is reused from the ViewModel cache.
 */
private data class ResolvedPathData(
    val source: PrebuiltPathData,
    val color: Color,
    val fillColor: Color
)