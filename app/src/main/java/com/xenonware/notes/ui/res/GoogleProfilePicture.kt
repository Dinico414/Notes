//package com.xenonware.notes.ui.res
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import com.xenonware.notes.R
//import com.xenonware.notes.presentation.sign_in.SignInState
//import com.xenonware.notes.presentation.sign_in.UserData
//
//@Composable
//fun GoogleProfilePicture(
//    state: SignInState,
//    userData: UserData?,
//    modifier: Modifier = Modifier
//) {
//    if (state.isSignInSuccessful && userData?.profilePictureUrl != null) {
//        AsyncImage(
//            model = userData.profilePictureUrl,
//            contentDescription = "Profile Picture",
//            modifier = modifier
//                .size(40.dp)
//                .clip(CircleShape),
//            contentScale = ContentScale.Crop
//        )
//    } else {
//        Image(
//            painter = painterResource(id = R.mipmap.default_icon),
//            contentDescription = stringResource(R.string.open_navigation_menu),
//            modifier = modifier.size(40.dp)
//        )
//    }
//}
