package com.peterlaurence.trekme.features.common.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextFieldCustom(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    label: String? = null,
    limit: Int? = null
) {
    var hasFocus by remember { mutableStateOf(false) }

    Column(modifier) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    hasFocus = it.hasFocus
                },
            value = text,
            label = {
                if (label != null) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Start,
                    )
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
            ),
            onValueChange = {
                if (limit == null || it.length <= limit) onTextChange(it)
            },
            singleLine = true,
            trailingIcon = {
                if (text.isNotEmpty() && hasFocus) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null
                        )
                    }
                }
            }
        )
        if (limit != null) {
            if (hasFocus) {
                Text(
                    text = "${text.length} / $limit",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                        .height(20.dp)
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primary
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}