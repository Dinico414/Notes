package com.xenonware.notes.ui.res

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.xenonware.notes.R

@Composable
fun DialogSignOut(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    XenonDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.sign_out),
        confirmButtonText = stringResource(R.string.confirm),
        onConfirmButtonClick = { onConfirm() },
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentManagesScrolling = false,
    ) {
        Text(
            text = stringResource(R.string.sign_out_dialog_description),
        )
    }
}
