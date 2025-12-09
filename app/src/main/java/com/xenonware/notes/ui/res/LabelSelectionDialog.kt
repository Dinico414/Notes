package com.xenonware.notes.ui.res

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonTextField
import com.xenon.mylibrary.values.MediumPadding
import com.xenonware.notes.R
import com.xenonware.notes.viewmodel.classes.Label

@Composable
fun LabelSelectionDialog(
    allLabels: List<Label>,
    selectedLabelId: String?,
    onLabelSelected: (String?) -> Unit,
    onAddNewLabel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newLabelName by remember { mutableStateOf("") }

    XenonDialog(
        onDismissRequest = onDismiss,
        title = "Choose a label",
        content = {
            Column {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLabelSelected(null) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLabelId == null,
                                onClick = { onLabelSelected(null) }
                            )
                            Text(
                                text = "None",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    items(allLabels) { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLabelSelected(label.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLabelId == label.id,
                                onClick = { onLabelSelected(label.id) }
                            )
                            Text(
                                text = label.text,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XenonTextField(
                        value = newLabelName,
                        onValueChange = { newLabelName = it },
                        placeholder = { Text(stringResource(R.string.add_new_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(modifier = Modifier.width(MediumPadding))
                    FilledIconButton(
                        onClick = {
                            if (newLabelName.isNotBlank()) {
                                onAddNewLabel(newLabelName.trim())
                                newLabelName = ""
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .width(40.dp),
                        enabled = newLabelName.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_new_label)
                        )
                    }
                }
            }
        }
    )
}