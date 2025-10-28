package com.xenonware.notes.ui.res

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

data class MenuItem(
    val text: String,
    val onClick: () -> Unit,
    val icon: (@Composable () -> Unit)? = null,
    val dismissOnClick: Boolean = true,
    val textColor: Color? = null,
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DropdownNoteMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MenuItem>,
    hazeState: HazeState,
) {
    val transitionState = remember {
        MutableTransitionState(initialState = false)
    }
    transitionState.targetState = expanded

    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = Alignment.TopEnd,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(
                    animationSpec = tween(
                        120,
                        easing = LinearOutSlowInEasing
                    )
                ) + expandVertically(
                    animationSpec = tween(120, easing = LinearOutSlowInEasing),
                    expandFrom = Alignment.Top
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        75,
                        easing = FastOutLinearInEasing
                    )
                ) + shrinkVertically(
                    animationSpec = tween(75, easing = FastOutLinearInEasing),
                    shrinkTowards = Alignment.Top
                )
            ) {
                val hazeThinColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceDim
                }
                Column(
                    modifier = Modifier
                        .padding(end = 4.dp, top = 64.dp)
                        .width(150.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .hazeEffect(
                            state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)
                        )
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.text, color = item.textColor ?: Color.Unspecified) },
                            onClick = {
                                item.onClick()
                                if (item.dismissOnClick) {
                                    onDismissRequest()
                                }
                            },
                            leadingIcon = item.icon
                        )
                    }
                }
            }
        }
    }
}