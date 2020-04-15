package com.peterlaurence.trekme.ui.record.components.events

import com.peterlaurence.trekme.ui.dialogs.EditFieldEvent
import kotlinx.android.parcel.Parcelize

@Parcelize
class RecordingNameChangeEvent(val _initialValue: String, val _newValue: String) : EditFieldEvent(_initialValue, _newValue)