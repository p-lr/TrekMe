package com.peterlaurence.trekme.core.map.domain.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ExcursionRef {
    val id: String
    val name: StateFlow<String>  // This flow is the same as the referred excursion
    val visible: MutableStateFlow<Boolean>
    val color: MutableStateFlow<String>
}

