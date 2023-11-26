package com.peterlaurence.trekme.features.record.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.peterlaurence.trekme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTopAppbar(
    onMainMenuClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.my_trails_title)) },
        navigationIcon = {
            IconButton(onClick = onMainMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
    )
}