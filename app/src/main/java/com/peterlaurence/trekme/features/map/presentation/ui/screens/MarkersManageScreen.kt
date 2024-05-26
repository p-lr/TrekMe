package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MarkersManageViewModel

@Composable
fun MarkersManageStateful(
    viewModel: MarkersManageViewModel = hiltViewModel(),
    onNavigateToMap: () -> Unit,
    onBackClick: () -> Unit
) {

}