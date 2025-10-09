package com.xenonware.notes.ui.res

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.notes.R
import com.xenonware.notes.ui.values.DialogPadding
import com.xenonware.notes.ui.values.LargerSpacing
import com.xenonware.notes.ui.values.LargestPadding
import com.xenonware.notes.ui.values.MediumPadding

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextNoteEditor(
    textState: String,
    onTextChange: (String) -> Unit,
    descriptionState: String,
    onDescriptionChange: (String) -> Unit,
    onSaveTask: () -> Unit,
    isSaveEnabled: Boolean,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = DialogPadding,
    bottomContentPadding: Dp = 0.dp,
    buttonRowHorizontalPadding: Dp = MediumPadding,
) {
    val scrollState = rememberScrollState()
    val topDividerAlpha by remember {
        derivedStateOf { if (scrollState.value > 0) 1f else 0f }
    }
    val bottomDividerAlpha by remember {
        derivedStateOf { if (scrollState.canScrollForward && scrollState.maxValue > 0) 1f else 0f }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeightForScrollableContent = screenHeight * 0.9f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier
                .alpha(topDividerAlpha)
                .padding(horizontal = horizontalContentPadding)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = maxHeightForScrollableContent)
                .padding(horizontal = horizontalContentPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            XenonTextField(
                value = textState,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.task_name)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )
            Spacer(modifier = Modifier.Companion.height(LargerSpacing))

            XenonTextField(
                value = descriptionState,
                onValueChange = onDescriptionChange,
                placeholder = { Text(stringResource(R.string.task_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = false,
                maxLines = 5
            )
            Spacer(modifier = Modifier.Companion.height(LargerSpacing))

            Spacer(modifier = Modifier.height(1.dp))
        }

        HorizontalDivider(
            modifier = Modifier
                .alpha(bottomDividerAlpha)
                .padding(horizontal = horizontalContentPadding)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = buttonRowHorizontalPadding)
                .padding(top = LargestPadding, bottom = bottomContentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onSaveTask, enabled = isSaveEnabled, modifier = Modifier.weight(1.5f)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontSize = 12.sp
                )
            }
        }
    }
}
