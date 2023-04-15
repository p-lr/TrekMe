package com.peterlaurence.trekme.core.map.domain.models

import kotlinx.coroutines.flow.MutableStateFlow

interface ExcursionRef {
    val id: String
    val name: MutableStateFlow<String>
    val visible: MutableStateFlow<Boolean>
    val color: MutableStateFlow<String>
}

