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

package org.emschu.snmp.cockpit.network;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.Log;

import com.google.zxing.client.result.WifiParsedResult;

import java.util.List;

import org.emschu.snmp.cockpit.BuildConfig;
import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;

/**
 * Singleton class to manage all network related actions of this app:
 * - get network information
 * - connect to new secure wifi
 * <p>
 * NOTE: only use the internal wifiManager instance, you should not create a new one
 * NOTE: this class handles an internal "mode" which should be updated if any network or preference
 * change occurs. {@link #updateMode()}
 * <p>
 * This class defines the central check if the app should be functional in {@link #isNetworkSecure()}.
 *
 * https://issuetracker.google.com/issues/129738210
 */
public class WifiNetworkManager {

    public static final String WIFI_SSID_AUTO = "[auto]";
    private static final String TAG = WifiNetworkManager.class.getName();
    public static final String ANDROID_WIFI_NAME = "AndroidWifi";
    @SuppressLint("StaticFieldLeak")
    private static WifiNetworkManager instance = null;
    private final Context context = SnmpCockpitApp.getContext();
    private final WifiManager wifiManager;
    private final CockpitPreferenceManager cockpitPreferenceManager;
    private MobileNetworkInformationService networkInfoService;

    // these execution modes of this manager exist: detect in in constructor.
    // can change during runtime.
    private final int MODE_WPA2_ONLY = 1;
    private final int MODE_FIXED_SSID = 2;
    private final int MODE_FIXED_SSID_WPA2_ONLY = 3;
    private final int MODE_DEFAULT_ANDROID_WIFI = 4;
    private int currentMode = 0;

    /**
     * constructor
     */
    private WifiNetworkManager() {
        this.cockpitPreferenceManager = SnmpCockpitApp.getPreferenceManager();

        updateMode();

        ConnectivityManager cm;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            this.networkInfoService = new WifiNetworkInformationService();
            this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                throw new IllegalStateException("could not get android connectivity manager " + ConnectivityManager.class.getName());
            }
        } else {
            this.wifiManager = null;
            cm = null;
            this.networkInfoService = null;
        }
    }

    /**
     * access method
     */
    public synchronized static WifiNetworkManager getInstance() {
        if (instance == null) {
            instance = new WifiNetworkManager();
        }
        return instance;
    }

    /**
     * should be called if network changes
     */
    public void updateMode() {
        int detectedMode = detectUserDefinedMode();
        if (detectedMode == 0) {
            throw new IllegalStateException("wifi network manager is not allowed to be 0");
        }
        Log.d(TAG, "wifi network manager mode: " + detectedMode);
        currentMode = detectedMode;
    }

    /**
     * raw string ip address of current wifi
     *
     * @return
     */
    public String getIpAddress() {
        if (this.networkInfoService == null) {
            return "-";
        }
        String[] ipv4Addr = this.networkInfoService.getIPv4Addresses();
        if (ipv4Addr == null || ipv4Addr.length == 0) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ipv4Addr.length; i++) {
            if (i != 0) {
                sb.append("\n");
            }
            sb.append(ipv4Addr[i]);
        }
        return sb.toString();
    }

    /**
     * get formatted current wifi ip address or null on failure
     *
     * @return
     */
    public String getIpAddressLabel() {
        return String.format(context.getResources().getString(R.string.nav_header_ip_label), getIpAddress());
    }

    /**
     * method to connect to a new network, defined in parsedResult
     *
     * @param parsedResult network credentials
     * @return success
     */
    public boolean connectNetwork(WifiParsedResult parsedResult) {
        if (wifiManager == null) {
            Log.e(TAG, "no wifi permissions!");
            return false;
        }
        if (parsedResult == null) {
            Log.e(TAG, "invalid null parsed result given");
            return false;
        }

        if (cockpitPreferenceManager.isWifiSSidLocked()
                && !cockpitPreferenceManager.isSSIDManuallyDefined()) {
            Log.d(TAG, "request update fixed ssid: " + parsedResult.getSsid());
            // update the current preference with the scan result
            cockpitPreferenceManager.updateFixedSSID(parsedResult.getSsid());
        }

        WifiConnectorService wcs = new WifiConnectorService();

        // enable wifi
        if (!wifiManager.isWifiEnabled()) {
            Log.w(TAG, "WIFI is not enabled!");
            wcs.suggestWifiEnabled();
        }

        wcs.connect(parsedResult);

        return false;
    }

    /**
     * this method is one of the hearts of this class
     * NOTE: the method could be called quite often and only this method considers all preferences
     * during execution
     *
     * @return if the definition of "wifi security" this app has is fulfilled
     */
    public boolean isNetworkSecure() {
        if (cockpitPreferenceManager.isAllNetworksAllowed()) {
            Log.d(TAG, "all networks allowed");
            return true;
        }

        if (!hasWifiConnection()) return false;

        if (currentMode == MODE_DEFAULT_ANDROID_WIFI) {
            Log.d(TAG, "accept default android network");
            return true;
        }
        if (currentMode == MODE_FIXED_SSID ||
                currentMode == MODE_FIXED_SSID_WPA2_ONLY) {
            // fixed ssid
            String currentSsid = getCurrentSsid();
            Log.d(TAG, "current ssid is: " + currentSsid);
            if (currentSsid == null || currentSsid.trim().isEmpty()) {
                Log.d(TAG, "no current ssid available");
                // we are not in wifi!
                return false;
            }
            boolean isSecureExpr = isConnectionSecure()
                    && ensureNetworkIsActive(currentSsid, cockpitPreferenceManager.getFixedWifiSSID());
            if (currentMode == MODE_FIXED_SSID) {
                return isSecureExpr;
            }
            return isSecureExpr;
        } else {
            // not fixed ssid
            return isConnectionSecure();
        }
    }

    public boolean hasWifiConnection() {
        MobileNetworkInformationService wis = new WifiNetworkInformationService();
        return wis.isConnectedToWifiNetwork();
    }

    public void refresh() {
        Log.d(TAG, "refreshing dhcp information");
        if (this.networkInfoService != null) {
            this.networkInfoService.refresh();
        } else {
            this.networkInfoService = new WifiNetworkInformationService();
        }
    }

    /**
     * method to retrieve current ssid we know
     *
     * @return
     */
    public String getCurrentSsid() {
        String ssid = this.networkInfoService.getSSID();
        if (ssid != null && !ssid.isEmpty()) {
            return ssid;
        }
        return "-";
    }

    public String getCurrentBssid() {
        String bssid = this.networkInfoService.getBSSID();
        if (bssid != null && !bssid.isEmpty()) {
            return bssid;
        }
        return "-";
    }

    /**
     * a method to check if a specific fixedSSID is connected right now
     *
     * @param fixedSSID valid wifi fixedSSID
     * @return
     */
    private boolean ensureNetworkIsActive(String currentSSID, String fixedSSID) {
        if (fixedSSID == null || fixedSSID.trim().isEmpty()) {
            Log.d(TAG, "invalid empty or null fixed ssid");
            return false;
        }
        Log.d(TAG, "fixedSSID retrieved: " + fixedSSID);
        Log.d(TAG, "compare with: " + currentSSID);

        if (BuildConfig.DEBUG && "\"AndroidWifi\"".equals(fixedSSID)) {
            Log.d(TAG, "allow android emulator wifi and define as secure in debug mode");
            return true;
        }
        if (currentSSID != null) {
            return currentSSID.equals(fixedSSID);
        }
        return false;
    }

    /**
     * disable device's wifi
     */
    public void disconnectWifi() {
        if (wifiManager == null) {
            Log.w(TAG, "No wifimanager due to not sufficient user permissions");
            return;
        }
        if (wifiManager.isWifiEnabled()) {
            WifiConnectorService wcs = new WifiConnectorService();
            wcs.suggestWifiDisabled();
        }
    }

    /**
     * get current wifi mode, see above for details
     *
     * @return
     */
    public int getCurrentMode() {
        return currentMode;
    }

    /**
     * internal method
     *
     * @return
     */
    private boolean isConnectionSecure() {
        String fixedSSID;
        if (currentMode == MODE_FIXED_SSID
                || currentMode == MODE_FIXED_SSID_WPA2_ONLY) {
            fixedSSID = cockpitPreferenceManager.getFixedWifiSSID();
            // user has to care about the ssid by default with these modes enabled
            if (fixedSSID == null || fixedSSID.isEmpty()) {
                return false;
            }
        } else {
            fixedSSID = getCurrentSsid();
        }
        // enables emulator network, where the checks below would not help
        if (BuildConfig.DEBUG && fixedSSID.equals(ANDROID_WIFI_NAME)) {
            // generally allow android emulator default wifi name in app debug mode
            Log.d(TAG, "always allow android wifi debug mode");
            return true;
        }

        if (currentMode != MODE_FIXED_SSID_WPA2_ONLY &&
                currentMode != MODE_WPA2_ONLY) {
            Log.d(TAG, "connection is secure. no wpa2 check (by user preference).");
            return true;
        }

        List<ScanResult> networkList = wifiManager.getScanResults();
        // filter scan result list for our ssid
        if (networkList != null && !networkList.isEmpty()) {
            for (ScanResult network : networkList) {
                //check if current connected SSID
                if (fixedSSID.equals(network.SSID.replace("\"", ""))) {
                    //get capabilities of current connection
                    String capabilities = network.capabilities;
                    if (capabilities.contains("WPA2") || capabilities.contains("WPA2-EAP")) {
                        return true;
                    }
                    break;
                }
            }
        }
        Log.w(TAG, "Could not check network security settings successfully");
        return false;
    }

    /**
     * detect the wifi connection (security) level
     *
     * TODO: use enum?!
     *
     * @return
     */
    private int detectUserDefinedMode() {
        boolean isSsidLocked = cockpitPreferenceManager.isWifiSSidLocked();
        boolean isWpa2Only = cockpitPreferenceManager.isWpa2Only();
        if (isWpa2Only) {
            if (isSsidLocked) {
                return MODE_FIXED_SSID_WPA2_ONLY;
            }
            return MODE_WPA2_ONLY;
        }
        if (isSsidLocked) {
            return MODE_FIXED_SSID;
        }
        return MODE_DEFAULT_ANDROID_WIFI;
    }

    /**
     * used in security overlay dialog
     * this is display logic
     *
     * @return
     */
    public String getCurrentModeLabel() {
        StringBuilder sb = new StringBuilder("\n");
        String fixedWifiSSID = cockpitPreferenceManager.getFixedWifiSSID();
        sb.append(context.getString(R.string.wifi_connection_list_item));
        sb.append("\n");
        // avoid "null" in ui
        if (fixedWifiSSID == null || fixedWifiSSID.isEmpty() || fixedWifiSSID.equals("null")) {
            // show "[auto]" only if we are in automatic mode
            if (cockpitPreferenceManager.isWifiSSidLocked() && !cockpitPreferenceManager.isSSIDManuallyDefined()) {
                fixedWifiSSID = WIFI_SSID_AUTO;
            } else {
                fixedWifiSSID = "";
            }
        }
        // FIXME this ugly code
        if (currentMode == MODE_DEFAULT_ANDROID_WIFI) {
        } else if (currentMode == MODE_FIXED_SSID_WPA2_ONLY) {
            sb.append(context.getString(R.string.fix_wifi_connection_ssid_list_item, fixedWifiSSID));
            sb.append("\n");
            sb.append(context.getString(R.string.wifi_connection_wpa_needed_list_item));
        } else if (currentMode == MODE_FIXED_SSID) {
            sb.append(context.getString(R.string.fix_wifi_connection_ssid_list_item, fixedWifiSSID));
        } else if (currentMode == MODE_WPA2_ONLY) {
            sb.append(context.getString(R.string.wifi_connection_wpa_needed_list_item));
        } else {
            throw new IllegalStateException("invalid current mode: " + currentMode);
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * to display ssid or "-" in drawer header
     *
     * @return
     */
    public String getCurrentSSIDLabel() {
        String currentSsid = getCurrentSsid();
        if (currentSsid == null || currentSsid.isEmpty()) {
            return context.getString(R.string.drawer_header_ssid_label, "-");
        }
        return context.getString(R.string.drawer_header_ssid_label, currentSsid);
    }

    /**
     * get string of dns server(s)
     *
     * @return
     */
    public String getDNSServer() {
        if (this.networkInfoService == null) {
            return "-";
        }
        String[] dnsServers = this.networkInfoService.getDNSServers();
        if (dnsServers == null || dnsServers.length == 0) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dnsServers.length; i++) {
            if (i != 0) {
                sb.append("\n");
            }
            sb.append(dnsServers[i]);
        }
        return sb.toString();
    }

    /**
     * returns internal wifi manager
     * nullable
     *
     * @return
     */
    public WifiManager getAndroidWifiManager() {
        return wifiManager;
    }

    public String getGateway() {
        if (this.networkInfoService == null) {
            return "-";
        }
        String gateway = this.networkInfoService.getGateway();
        if (gateway == null || gateway.isEmpty()) {
            return "-";
        }
        return gateway;
    }

    /**
     * method to retrieve ipv6 addresses of wlan0 of this smartphone without loopback
     * and "fe80" prefixed strings. addresses are separated by newlines "\n"
     *
     * @return
     */
    public String getIpv6Addresses() {
        if (this.networkInfoService == null) {
            return "-";
        }
        String[] ipv6Addrs = this.networkInfoService.getIPv6Addresses();
        if(ipv6Addrs == null || ipv6Addrs.length == 0) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ipv6Addrs.length; i++) {
            if (i != 0) {
                sb.append("\n");
            }
            sb.append(ipv6Addrs[i]);
        }
        return sb.toString();
    }

    public String getIpv6AddressLabel() {
        String ipv6AddrsLabel = getIpv6Addresses();
        return context.getString(R.string.drawer_header_ipv6_address_label, ipv6AddrsLabel);
    }

    /**
     * checks if the fixed ssid network is reachable: its part of wifi scan results
     *
     * @return
     */
    public boolean isTargetNetworkReachable() {
        if (!cockpitPreferenceManager.isWifiSSidLocked()) {
            // we know nothing about a SSID - if the user has not activated this preference
            return true;
        }
        String fixedSSID = cockpitPreferenceManager.getFixedWifiSSID();
        if (fixedSSID == null || fixedSSID.isEmpty() || fixedSSID.equals(WIFI_SSID_AUTO)) {
            return false;
        }
        if (this.wifiManager != null) {
            List<ScanResult> scanResults = this.wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                String ssid = scanResult.SSID.replace("\"", "");

                if(ssid.equals(fixedSSID) || ssid.equals(ANDROID_WIFI_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * trying to connect to target network
     */
    public void connectToTargetNetwork() {
        if (!cockpitPreferenceManager.isWifiSSidLocked()) {
            // we know nothing about a SSID - if the user has not activated this preference
            // FIXME open generic wifi connection dialog
            return;
        }
        Log.d(TAG, "connect to target network with fixed ssid: " + cockpitPreferenceManager.getFixedWifiSSID());
        String fixedSsid = cockpitPreferenceManager.getFixedWifiSSID();
        if (fixedSsid == null || fixedSsid.isEmpty() || fixedSsid.equals(WIFI_SSID_AUTO)) {
            Log.e(TAG, "connection attempt blocked. empty or invalid ssid.");
            return;
        }

        WifiConnectorService wcs = new WifiConnectorService();
        wcs.connect(fixedSsid);
    }

    @Nullable
    public MobileNetworkInformationService getNetInfoProvider() {
        return this.networkInfoService;
    }
}
