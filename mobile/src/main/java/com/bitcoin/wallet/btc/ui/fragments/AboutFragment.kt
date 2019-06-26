package com.bitcoin.wallet.btc.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.bitcoin.wallet.btc.R
import org.bitcoinj.core.VersionMessage

class AboutFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.config_about)
        findPreference<Preference>("about_credits_bitcoinj")?.title =
            getString(R.string.about_credits_bitcoinj_title, VersionMessage.BITCOINJ_VERSION)
    }
}