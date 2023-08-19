/*
 * snmp-cockpit
 *
 * Copyright (C) 2018-2023
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.emschu.snmp.cockpit.snmp.DeviceManager
import org.emschu.snmp.cockpit.snmp.MibCatalogManager

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        if (context == null || activity == null) {
            Log.d(TAG, "null context founds")
            return
        }
        val mibCatalogSelection =
            findPreference<Preference>(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION) as ListPreference?
        val context: Context = requireActivity()
        val mcm = MibCatalogManager(PreferenceManager.getDefaultSharedPreferences(context))
        val availableMibs: MutableList<String> = mutableListOf()
        for (mc in mcm.mibCatalog) {
            availableMibs.add(mc.catalogName)
        }
        mibCatalogSelection?.entries = availableMibs.toTypedArray()
        mibCatalogSelection?.entryValues = availableMibs.toTypedArray()
        val resetMibsPreference = findPreference<Preference>(CockpitPreferenceManager.KEY_MIB_CATALOG_RESET)
        resetMibsPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlertDialog.Builder(context)
                .setTitle(R.string.mib_catalog_reset_dialog_title)
                .setMessage(R.string.mib_catalog_reset_confirmation_message)
                .setPositiveButton(
                    android.R.string.ok
                ) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    mcm.resetToDefault(context.filesDir)
                    Toast.makeText(
                        activity, R.string.mib_catalog_reset_success_toast_message, Toast.LENGTH_LONG
                    )
                        .show()

                    // refresh preferences
                    preferenceScreen = null
                    addPreferencesFromResource(R.xml.pref_general)
//                        val sharedPreferences = this.context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
//
//                        CoroutineScope(Dispatchers.IO).launch {
//                            sharedPreferences?.let { MibCatalogManager(it) }
//                                ?.let { OIDCatalog.load(it) }
//                        }
                }
                .setNegativeButton(
                    android.R.string.cancel
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
                .show()
            false
        }

        val connectionTestRetries =
            preferenceScreen.findPreference<Preference>(
                CockpitPreferenceManager.KEY_CONNECTION_ATTEMPT_RETRIES
            ) as EditTextPreference?
        connectionTestRetries?.let {
            bindPreferenceSummaryToValue(it)
        }

        val connectionTestTimeout =
            preferenceScreen.findPreference<Preference>(
                CockpitPreferenceManager.KEY_CONNECTION_ATTEMPT_TIMEOUT
            ) as EditTextPreference?
        connectionTestTimeout?.let {
            bindPreferenceSummaryToValue(it)
        }

        val requestCounter =
            preferenceScreen.findPreference<Preference>(CockpitPreferenceManager.KEY_REQUEST_COUNTER)
        requestCounter?.let { bindPreferenceSummaryToValue(it) }


        val connectionTimeout =
            preferenceScreen.findPreference<Preference>(
                CockpitPreferenceManager.KEY_CONNECTION_TIMEOUT
            ) as EditTextPreference?
        connectionTimeout?.let {
            bindPreferenceSummaryToValue(it)
        }

        val connectionRetries =
            preferenceScreen.findPreference<Preference>(
                CockpitPreferenceManager.KEY_CONNECTION_RETRIES
            ) as EditTextPreference?
        connectionRetries?.let {
            bindPreferenceSummaryToValue(
                it
            )
        }

        findPreference<Preference>(CockpitPreferenceManager.KEY_REQUEST_COUNTER)?.summary =
            preferenceManager.sharedPreferences!!.getString(
                CockpitPreferenceManager.KEY_REQUEST_COUNTER, "0"
            ).toString()

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener { _, key: String? ->
                if (key == CockpitPreferenceManager.KEY_IS_V1_INSTEAD_OF_V2C) {
                    // check if the new value is v1 or v2c
                    val deviceManager: DeviceManager = SnmpCockpitApp.deviceManager
                    if (deviceManager.hasV1Connection() || deviceManager.hasV2Connection()) {
                        Log.d(TAG, "Remove all connections after preference change")
                        deviceManager.removeAllItems()
                    }
                }
            }
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see .sBindPreferenceSummaryToValueListener
     */
    private fun bindPreferenceSummaryToValue(preference: Preference) {
        Log.d(TAG, "registered " + preference.key + " preference")
        // Set the listener to watch for value changes.
        preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
            preference,
            PreferenceManager.getDefaultSharedPreferences(preference.context)
                .getString(preference.key, "")
        )
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private var sBindPreferenceSummaryToValueListener =
        Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
            val stringValue = newValue.toString()
            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                    if (index >= 0) preference.entries[index] else null
                )
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                when (preference.key) {
                    CockpitPreferenceManager.KEY_CONNECTION_ATTEMPT_RETRIES -> preference.summary =
                        preference.context.getString(R.string.pref_connection_test_retries_summary) + ": " + stringValue

                    CockpitPreferenceManager.KEY_CONNECTION_ATTEMPT_TIMEOUT -> preference.summary =
                        preference.context.getString(R.string.pref_connection_test_timeout_summary) + ": " + stringValue

                    CockpitPreferenceManager.KEY_CONNECTION_RETRIES -> preference.summary =
                        preference.context.getString(
                            R.string.pref_general_connection_retries_summary
                        ) + ": " + stringValue

                    CockpitPreferenceManager.KEY_CONNECTION_TIMEOUT -> preference.summary =
                        preference.context.getString(
                            R.string.pref_general_connection_timeout_summary
                        ) + ": " + stringValue

                    else -> preference.summary = stringValue
                }
            }
            true
        }

    companion object {
        private val TAG = SettingsActivity::class.simpleName
    }
}

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.preferences, SettingsFragment())
                .commit()
        }
        title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
}