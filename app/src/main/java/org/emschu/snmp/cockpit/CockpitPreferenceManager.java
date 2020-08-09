/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2018-2019
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.emschu.snmp.cockpit.network.MobileNetworkInformationService;
import org.emschu.snmp.cockpit.network.WifiNetworkManager;
import org.emschu.snmp.cockpit.query.OIDCatalog;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.MibCatalog;
import org.emschu.snmp.cockpit.snmp.MibCatalogManager;
import org.emschu.snmp.cockpit.util.BooleanObservable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static java.lang.Integer.parseInt;

/**
 * contains {@link GeneralPreferenceFragment} which handles app preferences
 */
public class CockpitPreferenceManager {
    static final String KEY_IS_WIFI_SSID_LOCKED = "is_wifi_ssid_locked";
    public static final String KEY_SECURE_WIFI_SSID = "secure_wifi_ssid";
    public static final String KEY_USE_CURRENT_WIFI_SSID = "use_current_ssid_btn";
    static final String KEY_IS_SSID_MANUAL = "is_ssid_manual";
    static final String KEY_IS_WPA2_ONLY = "is_wpa2_only";
    static final String KEY_SHOW_FLASHLIGHT_HINT = "show_flash_light_hint";
    static final String KEY_DEBUG_ALLOW_ALL_NETWORKS = "is_all_networks_allowed_debug";
    // connection detail user preferences
    static final String KEY_CONNECTION_ATTEMPT_RETRIES = "connection_test_retries";
    static final String KEY_CONNECTION_ATTEMPT_TIMEOUT = "connection_test_timeout";
    static final String KEY_CONNECTION_RETRIES = "connection_retries";
    static final String KEY_CONNECTION_TIMEOUT = "connection_timeout";
    static final String KEY_SESSION_TIMEOUT = "session_timeout";
    static final String KEY_IS_V1_INSTEAD_OF_V2C = "use_v1_instead_of_v2c";
    static final String KEY_PERIODIC_UI_UPDATE_ENABLED = "periodic_ui_update_enabled";
    static final String KEY_PERIODIC_UI_UPDATE_SECONDS = "ui_update_interval_seconds";
    static final String KEY_IPV6_LINK_LOCAL_DISPLAYED = "is_ipv6_link_local_displayed";
    static final String KEY_REQUEST_COUNTER = "request_action_counter";
    static final String KEY_BUILD_TIMESTAMP = "build_timestamp";
    static final String KEY_VERSION = "version";
    // mib catalog management
    public static final String KEY_MIB_CATALOG_SELECTION = "mib_catalog_selection";
    public static final String KEY_MIB_CATALOG_RESET = "mib_catalog_reset";
    // internal keys
    static final String KEY_INTERNAL_LAST_ACTIVITY_MS = "last_activity_in_ms";
    static final String KEY_PREF_VERSION = "pref_version_code";
    static final String KEY_SHOW_WELCOME_DIALOG = "welcome_dialog";

    // static prefs
    public static final int TIMEOUT_WAIT_ASYNC_MILLISECONDS_SHORT = 3000;
    public static final int TIMEOUT_WAIT_ASYNC_MILLISECONDS = 7500;

    private static final String[] PREFERENCE_KEYS = new String[]{
            KEY_IS_WIFI_SSID_LOCKED,
            KEY_SECURE_WIFI_SSID,
            KEY_IS_SSID_MANUAL,
            KEY_IS_WPA2_ONLY,
            KEY_DEBUG_ALLOW_ALL_NETWORKS,
            KEY_SHOW_FLASHLIGHT_HINT
    };
    static final String BAD_USER_INPUT_DETECTED = "Bad user input detected.";
    public static final String NOT_AN_INTEGER_GIVEN = "not an integer given!";
    private final SharedPreferences sharedPreferences;
    private static final String TAG = CockpitPreferenceManager.class.getName();

    /**
     * constructor
     *
     * @param context
     */
    public CockpitPreferenceManager(Context context) {
        sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);

        if (BuildConfig.VERSION_CODE > getPrefVersion()) {
            Log.d(TAG, "update pref version. reset request counter.");
            setPrefVersion(BuildConfig.VERSION_CODE);
        }
    }

    /**
     * helper method to reset request counter
     */
    public void resetRequestCounter() {
        Log.d(TAG, "reset request counter");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REQUEST_COUNTER, String.valueOf(0));
        editor.apply();
    }

    /**
     * increment request counter
     */
    public synchronized void incrementRequestCounter() {
        long oldValue;
        try {
            oldValue = parseInt(Objects.requireNonNull(sharedPreferences.getString(KEY_REQUEST_COUNTER, "0")));
        } catch (NumberFormatException | ClassCastException ignore) {
            sharedPreferences.edit().putString(KEY_REQUEST_COUNTER, "0").apply();
            oldValue = 0;
        }
        Log.d(TAG, "increment request counter to: " + (oldValue + 1));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REQUEST_COUNTER, String.valueOf(++oldValue));
        editor.apply();
    }

    /**
     * ui update interval
     *
     * @return
     */
    public int getUiUpdateSeconds() {
        int value;
        try {
            value = parseInt(Objects.requireNonNull(sharedPreferences.getString(KEY_PERIODIC_UI_UPDATE_SECONDS, "0")));
        } catch (NumberFormatException ignore) {
            value = 5;
        }
        // 3 is minimum
        if (value < 3) {
            return 3;
        }
        // 3000 secs max
        return Math.min(value, 3000);
    }

    public boolean isIpv6LinkLocalAddressesDisplayed() {
        return sharedPreferences.getBoolean(KEY_IPV6_LINK_LOCAL_DISPLAYED, true);
    }

    /**
     * helper method to set the version code pref
     * internal
     *
     * @param versionCode
     */
    private void setPrefVersion(int versionCode) {
        Log.d(TAG, "update pref version to: " + versionCode);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_PREF_VERSION, versionCode);
        editor.apply();
    }

    private int getPrefVersion() {
        return sharedPreferences.getInt(KEY_PREF_VERSION, 0);
    }

    /**
     * check session timeout
     */
    public void checkSessionTimeout() {
        if (DeviceManager.getInstance().getDeviceList().isEmpty()) {
            Log.d(TAG, "session timeout measurement is active only if devices are available");
            setLastActivity();
            return;
        }
        BooleanObservable isInSessionTimeoutObservable =
                CockpitStateManager.getInstance().getIsInSessionTimeoutObservable();
        // check for session timeout here
        long lastActivity = getLastActivity();
        if (lastActivity == 0) {
            // first time
            setLastActivity();
        } else {
            // calculate difference in secs
            long difference = ((System.currentTimeMillis() - lastActivity) / 1000);
            if (difference <= 10) {
                // this value is performance relevant!
                return;
            }
            Log.d(TAG, "activity difference (seconds): " + difference);
            boolean isNew = false;
            if ((getSessionTimeoutInMin() * 60) < difference) {
                // session timeout detected!
                isNew = true;
            }
            isInSessionTimeoutObservable.setValueAndTriggerObservers(isNew);
            // update
            setLastActivity();
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = (preference, newValue) -> {
        String stringValue = newValue.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);
        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            switch (preference.getKey()) {
                case KEY_CONNECTION_ATTEMPT_RETRIES:
                    preference.setSummary(preference.getContext()
                            .getString(R.string.pref_connection_test_retries_summary)
                            + ": " + stringValue);
                    break;
                case KEY_CONNECTION_ATTEMPT_TIMEOUT:
                    preference.setSummary(preference.getContext()
                            .getString(R.string.pref_connection_test_timeout_summary)
                            + ": " + stringValue);
                    break;
                case KEY_CONNECTION_RETRIES:
                    preference.setSummary(preference.getContext().getString(R.string.pref_general_connection_retries_summary)
                            + ": " + stringValue);
                    break;
                case KEY_CONNECTION_TIMEOUT:
                    preference.setSummary(preference.getContext().getString(R.string.pref_general_connection_timeout_summary)
                            + ": " + stringValue);
                    break;
                default:
                    preference.setSummary(stringValue);
                    break;
            }
        }
        return true;
    };

    /**
     * get preference value
     *
     * @return
     */
    public boolean isWifiSSidLocked() {
        return sharedPreferences.getBoolean(KEY_IS_WIFI_SSID_LOCKED, true);
    }

    /**
     * get preference value
     *
     * @return
     */
    public String getFixedWifiSSID() {
        return sharedPreferences.getString(KEY_SECURE_WIFI_SSID, null);
    }

    /**
     * get preference value
     *
     * @return
     */
    public boolean isPeriodicUpdateEnabled() {
        return sharedPreferences.getBoolean(KEY_PERIODIC_UI_UPDATE_ENABLED, false);
    }

    /**
     * get preference value
     *
     * @return
     */
    public boolean isSSIDManuallyDefined() {
        return sharedPreferences.getBoolean(KEY_IS_SSID_MANUAL, false);
    }

    /**
     * method to retrieve connection test timeout safely
     *
     * @return
     */
    public int getConnectionTestTimeout() {
        int prefValue = 0;
        try {
            prefValue = parseInt(sharedPreferences.getString(KEY_CONNECTION_ATTEMPT_TIMEOUT, "1000"));
        } catch (NumberFormatException ignore) {
            Log.w(TAG, NOT_AN_INTEGER_GIVEN);
        }
        if (prefValue == 0) {
            Log.w(TAG, "Bad user input detected. Connection test timeout too large. Using 1000 ms.");
            return 1000;
        }
        if (prefValue <= 100) {
            Log.w(TAG, "Bad user input detected. Connection test timeout too small. Using 100 ms.");
            // less than 100 ms is not a good idea..
            return 100;
        }
        if (prefValue >= 30000) {
            Log.w(TAG, "Bad user input detected. Connection test timeout too big. Using 30 s.");
            // limit to 30 s
            return 30000;
        }
        return prefValue;
    }

    /**
     * method to get a safe value for connection test reties
     *
     * @return
     */
    public int getConnectionTestRetries() {
        int prefValue = 0;
        try {
            prefValue = parseInt(sharedPreferences.getString(KEY_CONNECTION_ATTEMPT_RETRIES, "2"));
        } catch (NumberFormatException ignore) {
            Log.w(TAG, NOT_AN_INTEGER_GIVEN);
        }
        if (prefValue <= 0) {
            Log.w(TAG, "Bad user input detected. Connection test retries too small. Using 1.");
            return 1;
        }
        if (prefValue > 25) {
            Log.w(TAG, "Bad user input detected. Connection test retries too big. Using 25.");
            return 25;
        }
        return prefValue;
    }

    /**
     * get connection retries
     *
     * @return
     */
    public int getConnectionRetries() {
        int prefValue = 3;
        try {
            prefValue = parseInt(sharedPreferences.getString(KEY_CONNECTION_RETRIES, "3"));
        } catch (NumberFormatException ignore) {
            Log.w(TAG, NOT_AN_INTEGER_GIVEN);
        }

        if (prefValue < 1) {
            Log.w(TAG, BAD_USER_INPUT_DETECTED);
            prefValue = 1;
        }
        if (prefValue > 10) {
            Log.w(TAG, BAD_USER_INPUT_DETECTED);
            prefValue = 10;
        }
        return prefValue;
    }

    /**
     * method to get connection timeout in ms
     *
     * @return
     */
    public int getConnectionTimeout() {
        int prefValue = 5000;
        try {
            prefValue = parseInt(sharedPreferences.getString(KEY_CONNECTION_TIMEOUT, "5000"));
        } catch (NumberFormatException ignore) {
            Log.w(TAG, NOT_AN_INTEGER_GIVEN);
        }
        if (prefValue < 1000) {
            Log.w(TAG, BAD_USER_INPUT_DETECTED);
            prefValue = 1000;
        }
        if (prefValue > 20000) {
            Log.w(TAG, BAD_USER_INPUT_DETECTED);
            prefValue = 20000;
        }
        return prefValue;
    }

    public boolean isWpa2Only() {
        return sharedPreferences.getBoolean(KEY_IS_WPA2_ONLY, false);
    }

    public boolean isAllNetworksAllowed() {
        return sharedPreferences.getBoolean(KEY_DEBUG_ALLOW_ALL_NETWORKS, false);
    }

    /**
     * update ssid method
     *
     * @param ssid
     */
    public void updateFixedSSID(String ssid) {
        if (ssid == null) {
            throw new IllegalArgumentException("null ssid not allowed");
        }
        Log.i(TAG, "Update fixed ssid to: " + ssid);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(KEY_SECURE_WIFI_SSID, ssid);
        edit.apply();
    }

    public static String[] getAllPreferenceKeys() {
        return PREFERENCE_KEYS;
    }

    /**
     * indicator whether to show the flashlisght hint or not
     *
     * @return
     */
    public boolean showFlashlightHint() {
        return sharedPreferences.getBoolean(CockpitPreferenceManager.KEY_SHOW_FLASHLIGHT_HINT, true);
    }

    /**
     * helper method to disable flashlight hint dialogs app-wide
     */
    public void disableFlashLightHint() {
        Log.d(TAG, "Flashlight hint is disabled");
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(KEY_SHOW_FLASHLIGHT_HINT, false);
        edit.apply();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    /**
     * last session activity
     *
     * @return
     */
    public long getLastActivity() {
        return sharedPreferences.getLong(CockpitPreferenceManager.KEY_INTERNAL_LAST_ACTIVITY_MS, 0);
    }

    /**
     * sets current moment as last activity. this is important for user session timeout feature
     */
    public void setLastActivity() {
        final long currentTime = System.currentTimeMillis();
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putLong(CockpitPreferenceManager.KEY_INTERNAL_LAST_ACTIVITY_MS, currentTime);
        edit.apply();
    }

    /**
     * session timeout in min
     *
     * @return
     */
    public int getSessionTimeoutInMin() {
        try {
            return parseInt(sharedPreferences.getString(CockpitPreferenceManager.KEY_SESSION_TIMEOUT, "60"));
        } catch (NumberFormatException ignore) {
            Log.w(TAG, "could not get session timeout value");
        }
        // return default
        return 60;
    }

    /**
     * user pref
     *
     * @return
     */
    public boolean isV1InsteadOfV3() {
        return sharedPreferences.getBoolean(KEY_IS_V1_INSTEAD_OF_V2C, false);
    }

    public boolean isWelcomeScreenShown() {
        return sharedPreferences.getBoolean(KEY_SHOW_WELCOME_DIALOG, false);
    }

    public void setWelcomeScreenShown() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(KEY_SHOW_WELCOME_DIALOG, true);
        edit.apply();
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.pref_general);
            PackageInfo packageInfo;
            try {
                if (getContext() == null) {
                    Log.d(TAG, "null context founds");
                    return;
                }

                packageInfo = getContext().getPackageManager().
                        getPackageInfo(getContext().getPackageName(), 0);

                String version = packageInfo.versionName;
                long versionCode = PackageInfoCompat.getLongVersionCode(packageInfo);
                findPreference(KEY_VERSION)
                        .setSummary(getContext().getString(R.string.preference_version, version, versionCode));

                String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss",
                        getCurrentLocale(getContext())).format(new Date(BuildConfig.BUILD_TIMESTAMP));
                findPreference(KEY_BUILD_TIMESTAMP).setSummary(getString(R.string.timestamp, timestamp));
                ListPreference mibCatalogSelection = (ListPreference) findPreference(KEY_MIB_CATALOG_SELECTION);

                Context context = getActivity();
                MibCatalogManager mcm = new MibCatalogManager(androidx.preference.PreferenceManager.getDefaultSharedPreferences(context));
                List<String> availableMibs = new ArrayList<>();
                for (MibCatalog mc : mcm.getMibCatalog()) {
                    availableMibs.add(mc.getCatalogName());
                }
                mibCatalogSelection.setEntries(availableMibs.toArray(new String[]{}));
                mibCatalogSelection.setEntryValues(availableMibs.toArray(new String[]{}));

                Preference resetMibsPreference = findPreference(KEY_MIB_CATALOG_RESET);
                resetMibsPreference.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.mib_catalog_reset_dialog_title)
                            .setMessage(R.string.mib_catalog_reset_confirmation_message)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                dialog.dismiss();
                                mcm.resetToDefault(context.getFilesDir());
                                Toast.makeText(getActivity(), R.string.mib_catalog_reset_success_toast_message, Toast.LENGTH_LONG).show();

                                // refresh preferences
                                setPreferenceScreen(null);
                                addPreferencesFromResource(R.xml.pref_general);
                                AsyncTask.execute(() -> OIDCatalog.getInstance(null, null).refresh());
                            })
                            .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss()).create().show();
                    return false;
                });

                final EditTextPreference secureSSIDPref = findPreference(KEY_SECURE_WIFI_SSID);

                Preference setWifiSSIDToCurrent = findPreference(KEY_USE_CURRENT_WIFI_SSID);
                bindPreferenceSummaryToValue(setWifiSSIDToCurrent);

                setWifiSSIDToCurrent.setOnPreferenceClickListener(preference -> {
                    MobileNetworkInformationService mobileNetworkInformationService = WifiNetworkManager.getInstance().getNetInfoProvider();
                    if (mobileNetworkInformationService != null) {
                        final String currentSSID = mobileNetworkInformationService.getSSID();
                        if (currentSSID != null && !currentSSID.isEmpty()) {
                            secureSSIDPref.setText(currentSSID);
                            secureSSIDPref.callChangeListener(currentSSID);
                        } else {
                            Toast.makeText(getActivity(), R.string.not_connected_label, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.not_connected_label, Toast.LENGTH_LONG).show();
                    }
                    return true;
                });
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "preference build timestamp name not found: " + e.getMessage());
            }

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            EditTextPreference secureWifiSSID = (EditTextPreference)
                    getPreferenceScreen().findPreference(KEY_SECURE_WIFI_SSID);
            bindPreferenceSummaryToValue(secureWifiSSID);

            EditTextPreference connectionTestRetries =
                    (EditTextPreference) getPreferenceScreen().findPreference(KEY_CONNECTION_ATTEMPT_RETRIES);
            bindPreferenceSummaryToValue(connectionTestRetries);

            EditTextPreference connectionTestTimeout =
                    (EditTextPreference) getPreferenceScreen().findPreference(KEY_CONNECTION_ATTEMPT_TIMEOUT);
            bindPreferenceSummaryToValue(connectionTestTimeout);

            Preference requestCounter = getPreferenceScreen().findPreference(KEY_REQUEST_COUNTER);
            bindPreferenceSummaryToValue(requestCounter);

            EditTextPreference connectionTimeout = (EditTextPreference) getPreferenceScreen()
                    .findPreference(KEY_CONNECTION_TIMEOUT);
            bindPreferenceSummaryToValue(connectionTimeout);

            EditTextPreference connectionRetries = (EditTextPreference) getPreferenceScreen()
                    .findPreference(KEY_CONNECTION_RETRIES);
            bindPreferenceSummaryToValue(connectionRetries);

            findPreference(KEY_REQUEST_COUNTER)
                    .setSummary(
                            String.valueOf(getPreferenceManager()
                                    .getSharedPreferences().getString(KEY_REQUEST_COUNTER, "0"))
                    );

            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
                        if (key.equals(KEY_IS_WIFI_SSID_LOCKED)
                                || key.equals(KEY_IS_SSID_MANUAL)
                                || key.equals(KEY_SECURE_WIFI_SSID)
                                || key.equals(KEY_DEBUG_ALLOW_ALL_NETWORKS)
                                || key.equals(KEY_IS_WPA2_ONLY)) {
                            WifiNetworkManager networkManager = WifiNetworkManager.getInstance();
                            networkManager.updateMode();
                        }

                        if (key.equals(KEY_PERIODIC_UI_UPDATE_ENABLED) && getContext() instanceof CockpitMainActivity) {
                            ((CockpitMainActivity) getContext()).initTasks();
                        }
                        if (key.equals(KEY_IS_V1_INSTEAD_OF_V2C)) {
                            // check if the new value is v1 or v2c
                            DeviceManager deviceManager = DeviceManager.getInstance();
                            if (deviceManager.hasV1Connection()
                                    || deviceManager.hasV2Connection()) {
                                Log.d(TAG, "Remove all connections after preference change");
                                deviceManager.removeAllItems();
                            }
                        }
                    });
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see #sBindPreferenceSummaryToValueListener
         */
        private static void bindPreferenceSummaryToValue(Preference preference) {
            Log.d(TAG, "registered " + preference.getKey() + " preference");
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        /**
         * method for current locale
         *
         * @param context
         * @return
         */
        @SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
        protected Locale getCurrentLocale(Context context) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                //noinspection deprecation
                return context.getResources().getConfiguration().locale;
            } else {
                return context.getResources().getConfiguration().getLocales().get(0);
            }
        }
    }

}
