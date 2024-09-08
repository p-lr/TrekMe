package com.peterlaurence.trekme.core.map.domain.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ExcursionRef {
    val id: String
    val name: StateFlow<String>
    val visible: MutableStateFlow<Boolean>
    val color: MutableStateFlow<String>
}

