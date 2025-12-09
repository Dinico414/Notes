package com.xenonware.notes.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.notes.R


@Composable
fun FilterItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    val backgroundColor = if (isSelected) {
        colorScheme.inversePrimary
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100f))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(start = LargestPadding, end = SmallPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {

        Row(
            modifier = Modifier
                .padding(vertical = LargestPadding)
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(LargestPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = colorScheme.onSurface
            )
            Text(
                text = label,
                fontFamily = QuicksandTitleVariable,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        onDeleteClick?.let {
            IconButton(
                onClick = it
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    tint = colorScheme.onSurface,
                    contentDescription = stringResource(R.string.remove_step)
                )
            }
        }
    }
}