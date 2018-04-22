package com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign

import android.os.Bundle
import android.preference.PreferenceFragment
import com.peterlaurence.trekadvisor.R

class IgnCredentialsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.ign_credentials_settings)
    }
}