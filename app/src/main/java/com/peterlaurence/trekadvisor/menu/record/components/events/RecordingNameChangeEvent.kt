package com.peterlaurence.trekadvisor.menu.record.components.events

import com.peterlaurence.trekadvisor.menu.dialogs.EditFieldEvent


class RecordingNameChangeEvent(initialValue: String, newValue: String) : EditFieldEvent(initialValue, newValue)