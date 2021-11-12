package com.peterlaurence.trekme.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@Composable
fun MapTopAppBar(
    isShowingOrientation: Boolean,
    onMenuClick: () -> Unit,
    onToggleShowOrientation: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Box(
                Modifier
                    .height(24.dp)
                    .wrapContentSize(Alignment.BottomEnd, true)
            ) {
                DropdownMenu(
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(onClick = onToggleShowOrientation) {
                        Text(stringResource(id = R.string.mapview_orientation_enable))
                        Spacer(Modifier.width(8.dp))
                        Checkbox(
                            checked = isShowingOrientation,
                            onCheckedChange = { onToggleShowOrientation() })
                    }
                }
            }
        }
    )

}