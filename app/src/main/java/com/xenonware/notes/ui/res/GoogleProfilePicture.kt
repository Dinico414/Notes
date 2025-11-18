package com.xenonware.notes.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xenonware.notes.R

@Composable
fun GoogleProfilePicture(
    modifier: Modifier
) {
    if (state.isSignedIn) {
        Image(
            painter = painterResource(id = R.mipmap.default_icon),
            contentDescription = stringResource(R.string.open_navigation_menu),
            modifier = Modifier.size(40.dp)
        )
    } else {
        AsyncImage(
            model = userData.profilePicture,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}