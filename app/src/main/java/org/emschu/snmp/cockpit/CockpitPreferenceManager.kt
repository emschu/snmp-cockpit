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
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

/**
 * app preference related code
 */
open class CockpitPreferenceManager(
    private val context: Context,
    val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
) {

    companion object {
        // connection detail user preferences
        const val KEY_CONNECTION_ATTEMPT_RETRIES = "connection_test_retries"
        const val KEY_CONNECTION_ATTEMPT_TIMEOUT = "connection_test_timeout"
        const val KEY_CONNECTION_RETRIES = "connection_retries"
        const val KEY_CONNECTION_TIMEOUT = "connection_timeout"
        const val KEY_IS_V1_INSTEAD_OF_V2C = "use_v1_instead_of_v2c"
        const val KEY_PERIODIC_UI_UPDATE_ENABLED = "periodic_ui_update_enabled"
        const val KEY_PERIODIC_UI_UPDATE_SECONDS = "ui_update_interval_seconds"
        const val KEY_IPV6_LINK_LOCAL_DISPLAYED = "is_ipv6_link_local_displayed"
        const val KEY_REQUEST_COUNTER = "request_action_counter"

        // mib catalog management
        const val KEY_MIB_CATALOG_SELECTION = "mib_catalog_selection"
        const val KEY_MIB_CATALOG_RESET = "mib_catalog_reset"
        const val KEY_MIB_CATALOG_CONTENT = "mib_catalog_content"

        // internal keys
        const val KEY_INTERNAL_LAST_ACTIVITY_MS = "last_activity_in_ms"
        const val KEY_PREF_VERSION = "pref_version_code"
        const val KEY_SHOW_WELCOME_DIALOG = "welcome_dialog"

        // static prefs
        const val BAD_USER_INPUT_DETECTED = "Bad user input detected."
        const val NOT_AN_INTEGER_GIVEN = "not an integer given!"
        private val TAG = CockpitPreferenceManager::class.java.name
    }

    /**
     * helper method to set the version code pref
     * internal
     */
    private var prefVersion: Int
        get() = sharedPreferences.getInt(KEY_PREF_VERSION, 0)
        set(versionCode) {
            Log.d(TAG, "update pref version to: $versionCode")
            val editor = sharedPreferences.edit()
            editor.putInt(KEY_PREF_VERSION, versionCode)
            editor.apply()
        }

    init {
        if (BuildConfig.VERSION_CODE > prefVersion) {
            Log.d(TAG, "update pref version. reset request counter.")
            prefVersion = BuildConfig.VERSION_CODE
        }
    }

    /**
     * increment request counter
     */
    fun incrementRequestCounter() {
        var oldValue: Long = try {
            sharedPreferences.getString(KEY_REQUEST_COUNTER, "0")!!
                .toInt()
                .toLong()
        } catch (ignore: NumberFormatException) {
            sharedPreferences.edit()
                .putString(KEY_REQUEST_COUNTER, "0")
                .apply()
            0
        } catch (ignore: ClassCastException) {
            sharedPreferences.edit()
                .putString(KEY_REQUEST_COUNTER, "0")
                .apply()
            0
        }
        Log.d(TAG, "increment request counter to: " + (oldValue + 1))
        val editor = sharedPreferences.edit()
        oldValue += 1
        editor.putString(KEY_REQUEST_COUNTER, oldValue.toString())
        editor.apply()
    }

    /**
     * ui update interval
     *
     * @return
     */
    val uiUpdateSeconds: Int
        get() {
            val value: Int = try {
                (sharedPreferences.getString(KEY_PERIODIC_UI_UPDATE_SECONDS, "0") ?: "").toInt()
            } catch (ignore: NumberFormatException) {
                5
            }
            // 3 is minimum
            return if (value < 3) {
                3
            } else value.coerceAtMost(3000)
            // 3000 secs max
        }

    val isIpv6LinkLocalAddressesDisplayed: Boolean
        get() = sharedPreferences.getBoolean(KEY_IPV6_LINK_LOCAL_DISPLAYED, true)

    /**
     * get preference value
     *
     * @return
     */
    val isPeriodicUpdateEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_PERIODIC_UI_UPDATE_ENABLED, false)

    /**
     * method to retrieve connection test timeout safely
     *
     * @return
     */
    val connectionTestTimeout: Int
        get() {
            var prefValue = 0
            try {
                prefValue = (sharedPreferences.getString(KEY_CONNECTION_ATTEMPT_TIMEOUT, "1000") ?: "").toInt()
            } catch (ignore: NumberFormatException) {
                Log.w(TAG, NOT_AN_INTEGER_GIVEN)
            }
            if (prefValue == 0) {
                Log.w(TAG, "Bad user input detected. Connection test timeout too large. Using 1000 ms.")
                return 1000
            }
            if (prefValue <= 100) {
                Log.w(TAG, "Bad user input detected. Connection test timeout too small. Using 100 ms.")
                // less than 100 ms is not a good idea..
                return 100
            }
            if (prefValue >= 30000) {
                Log.w(TAG, "Bad user input detected. Connection test timeout too big. Using 30 s.")
                // limit to 30 s
                return 30000
            }
            return prefValue
        }

    /**
     * method to get a safe value for connection test reties
     *
     * @return
     */
    val connectionTestRetries: Int
        get() {
            var prefValue = 1
            try {
                prefValue = sharedPreferences.getString(KEY_CONNECTION_ATTEMPT_RETRIES, "2")!!
                    .toInt()
            } catch (ignore: NumberFormatException) {
                Log.w(TAG, NOT_AN_INTEGER_GIVEN)
            }
            if (prefValue <= 0) {
                Log.w(TAG, "Bad user input detected. Connection test retries too small. Using 1.")
                return 1
            }
            if (prefValue > 25) {
                Log.w(TAG, "Bad user input detected. Connection test retries too big. Using 25.")
                return 25
            }
            return prefValue
        }

    /**
     * get connection retries
     *
     * @return
     */
    val connectionRetries: Int
        get() {
            var prefValue = 3
            try {
                prefValue = sharedPreferences.getString(KEY_CONNECTION_RETRIES, "2")!!
                    .toInt()
            } catch (ignore: NumberFormatException) {
                Log.w(TAG, NOT_AN_INTEGER_GIVEN)
            }
            if (prefValue < 1) {
                Log.w(TAG, BAD_USER_INPUT_DETECTED)
                prefValue = 1
            }
            if (prefValue > 10) {
                Log.w(TAG, BAD_USER_INPUT_DETECTED)
                prefValue = 10
            }
            return prefValue
        }

    /**
     * method to get connection timeout in ms
     *
     * @return
     */
    val connectionTimeout: Int
        get() {
            var prefValue = 5000
            try {
                prefValue = sharedPreferences.getString(KEY_CONNECTION_TIMEOUT, "5000")!!
                    .toInt()
            } catch (ignore: NumberFormatException) {
                Log.w(TAG, NOT_AN_INTEGER_GIVEN)
            }
            if (prefValue < 1000) {
                Log.w(TAG, BAD_USER_INPUT_DETECTED)
                prefValue = 1000
            }
            if (prefValue > 20000) {
                Log.w(TAG, BAD_USER_INPUT_DETECTED)
                prefValue = 20000
            }
            return prefValue
        }

    /**
     * last session activity
     *
     * @return
     */
    private val lastActivity: Long = try {
        sharedPreferences.getLong(KEY_INTERNAL_LAST_ACTIVITY_MS, 0)
    } catch (ex: NumberFormatException) {
        0L
    }

    /**
     * sets current moment as last activity. this is important for user session timeout feature
     */
    private fun updateLastActivity() = sharedPreferences.edit()
        .putLong(KEY_INTERNAL_LAST_ACTIVITY_MS, System.currentTimeMillis())
        .apply()

    val isV1InsteadOfV2: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_V1_INSTEAD_OF_V2C, false)
    val isWelcomeScreenShown: Boolean
        get() = sharedPreferences.getBoolean(KEY_SHOW_WELCOME_DIALOG, false)

    fun setWelcomeScreenShown() = sharedPreferences.edit()
        .putBoolean(KEY_SHOW_WELCOME_DIALOG, true)
        .apply()
}