package com.gelakinetic.telekm

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class MousePreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}