package com.xenonware.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xenonware.notes.viewmodel.DrawingAction

@Composable
fun NoteControls(
    selectedColor: Color,
    colors: List<Color>,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 34.dp
    val colorButtonSize = 28.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(colorButtonSize)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (isSelected) 2.dp else 0.8.dp,
                            Color.Gray,
                            shape = CircleShape
                        )
                        .clickable {
                            onAction(DrawingAction.SelectColor(color))
                        }
                )
            }
        }
        IconButton(
            onClick = { onAction(DrawingAction.ClearCanvas) },
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
//                tint = Color.White
                modifier = Modifier
                    .scale(-1f, 1f)
            )
        }
    }
}