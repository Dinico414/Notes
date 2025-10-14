package com.xenonware.notes.ui.res

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import com.xenonware.notes.ui.values.ExtraLargestPadding
import com.xenonware.notes.ui.values.MediumCornerRadius
import com.xenonware.notes.ui.values.MediumSpacing
import com.xenonware.notes.viewmodel.classes.NotesItems

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellListNote(
    item: NotesItems,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onSelectItem: () -> Unit,
    onEditItem: (NotesItems) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "Border Color Animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MediumCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .border(
                width = 2.dp, color = borderColor, shape = RoundedCornerShape(MediumCornerRadius)
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
            modifier = Modifier.padding(ExtraLargestPadding)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = QuicksandTitleVariable,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(MediumSpacing))
                val listItems = try {
                    item.description.split("\n").filter { it.isNotBlank() }
                } catch (e: Exception) {
                    listOf(item.description)
                }

                listItems.take(maxLines).forEach { listItemText ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = listItemText.trim(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (listItemText.startsWith("[x]")) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
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
                    .padding(4.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = isSelected, label = "Selection Animation") { selected ->
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(20.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 4.dp, end = 4.dp)
                .size(48.dp), contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = "List",
                    tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }
        }
    }
}
