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
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.client.result.WifiParsedResult;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.emschu.snmp.cockpit.BuildConfig;
import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.util.Converter;

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
 */
public class WifiNetworkManager {

    public static final String UNKNOWN_SSID_KEY = "<unknown ssid>";
    public static final String WIFI_SSID_AUTO = "[auto]";
    private static String TAG = WifiNetworkManager.class.getName();
    private static WifiNetworkManager wifiNetworkManagerInstance = null;
    private Context context;
    private DhcpInfo dhcpInfo;
    private final WifiManager wifiManager;
    private static final int[] secureProtocols = new int[]{
            WifiConfiguration.KeyMgmt.WPA_PSK,
            WifiConfiguration.KeyMgmt.WPA_EAP,
            WifiConfiguration.KeyMgmt.IEEE8021X
    };
    private final ConnectivityManager cm;
    private final CockpitPreferenceManager cockpitPreferenceManager;

    // these execution modes of this manager exist: detect in in constructor.
    // can change during runtime.
    private int MODE_WPA2_ONLY = 1;
    private int MODE_FIXED_SSID = 2;
    private int MODE_FIXED_SSID_WPA2_ONLY = 3;
    private int MODE_DEFAULT_ANDROID_WIFI = 4;
    private int currentMode = 0;

    /**
     * constructor
     *
     * @param context
     */
    @SuppressWarnings({"WifiManagerPotentialLeak"})
    private WifiNetworkManager(Context context) {
        this.context = context;
        this.cockpitPreferenceManager = SnmpCockpitApp.getPreferenceManager();

        updateMode();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                dhcpInfo = wifiManager.getDhcpInfo();
            } else {
                Log.w(TAG, "null wifi manager!");
            }

            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                throw new IllegalStateException("could not get android connectivity manager " + ConnectivityManager.class.getName());
            }
        } else {
            wifiManager = null;
            cm = null;
        }
    }

    /**
     * access method
     */
    public static WifiNetworkManager getInstance(Context context) {
        if (wifiNetworkManagerInstance == null) {
            wifiNetworkManagerInstance = new WifiNetworkManager(context.getApplicationContext());
        }
        return wifiNetworkManagerInstance;
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
        if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
            return Converter.intToIp(dhcpInfo.ipAddress);
        }
        return "-";
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
     * ip string of the current subnet
     *
     * @return
     */
    public String getSubnetMask() {
        if (dhcpInfo != null && dhcpInfo.netmask != 0) {
            return Converter.intToIp(dhcpInfo.netmask);
        }
        return "-";
    }

    /**
     * get formatted current wifi ip subnet or null on failure
     *
     * @return
     */
    public String getSubnetMaskLabel() {
        return String.format(context.getResources().getString(R.string.nav_header_subnet_label), getSubnetMask());
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
        // enable wifi
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            Toast.makeText(context, R.string.wifi_activated_toast_label, Toast.LENGTH_SHORT).show();
        }
        if (cockpitPreferenceManager.isWifiSSidLocked()
                && !cockpitPreferenceManager.isSSIDManuallyDefined()) {
            Log.d(TAG, "request update fixed ssid: " + parsedResult.getSsid());
            // update the current preference with the scan result
            cockpitPreferenceManager.updateFixedSSID(parsedResult.getSsid());
        }

        WifiConnector connector = new WifiConnector(parsedResult);
        if (connector.canConnect()) {
            connector.connect(wifiManager);

            if (connector.isFailed()) {
                return false;
            }
            return true;
        } else {
            Log.w(TAG, "invalid network type");
        }
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
            if (currentMode == MODE_FIXED_SSID) {
                return isConnectionSecure() && isThisSSIDTheCurrent(cockpitPreferenceManager.getFixedWifiSSID());
            }
            return isConnectionSecure()
                    && isThisSSIDTheCurrent(cockpitPreferenceManager.getFixedWifiSSID());
        } else {
            // not fixed ssid
            return isConnectionSecure();
        }
    }

    public boolean hasWifiConnection() {
        // general check
        if (!hasNetworkAccess()) {
            Log.d(TAG, "no network access");
            return false;
        }
        // wifi check
        boolean isWifiConnected = false;

        for (Network activeNetwork : cm.getAllNetworks()) {
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    isWifiConnected = true;
                }
            }
        }
        if (!isWifiConnected) {
            Log.d(TAG, "not a wifi network here!");
            return false;
        }
        return true;
    }

    public void refresh() {
        Log.i(TAG, "refreshing dhcp information");
        dhcpInfo = wifiManager.getDhcpInfo();
    }

    /**
     * method to retrieve current ssid we know
     *
     * @return
     */
    public String getCurrentSsid() {
        if (!hasNetworkAccess()) {
            Log.d(TAG, "no network access");
            return "-";
        }
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null) {
                String ssid = info.getExtraInfo();
                Log.v(TAG, "WiFi SSID of android network info: " + ssid);
                if (ssid == null || ssid.isEmpty()) {
                    return "-";
                }
                if (ssid.equals("internet")) {
                    Log.d(TAG, "no wifi network detected!");
                    return "-";
                }
                return ssid.replace("\"", "");
            }
        }
        return "-";
    }

    /**
     * a method to check if a specific fixedSSID is connected right now
     *
     * @param fixedSSID valid wifi fixedSSID
     * @return
     */
    private boolean isThisSSIDTheCurrent(String fixedSSID) {
        if (fixedSSID == null || fixedSSID.trim().isEmpty()) {
            Log.d(TAG, "invalid empty or null fixed ssid");
            return false;
        }
        String currentSsid = getCurrentSsid();
        Log.d(TAG, "fixedSSID retrieved: " + currentSsid);
        Log.d(TAG, "compare with: " + fixedSSID);
        if ("\"AndroidWifi\"".equals(fixedSSID) && BuildConfig.DEBUG) {
            Log.d(TAG, "allow android emulator wifi and define as secure");
            return true;
        }
        if (currentSsid != null) {
            return currentSsid.equals(fixedSSID);
        }
        return false;
    }

    /**
     * check if network access exists for this app
     *
     * @return
     */
    public boolean hasNetworkAccess() {
        if (cm == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
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
            wifiManager.setWifiEnabled(false);
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
    private final boolean isConnectionSecure() {
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
        if (getCurrentSsid().equals("AndroidWifi") && BuildConfig.DEBUG) {
            // generally allow android emulator default wifi name in app debug mode
            Log.d(TAG, "allow android wifi");
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
        return checkWithKnownNetworks(fixedSSID);
    }

    /**
     * helper method
     *
     * @param fixedSSID
     * @return
     */
    private boolean checkWithKnownNetworks(String fixedSSID) {
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        if (wifiConfigurationList == null || wifiConfigurationList.isEmpty()) {
            Log.d(TAG, "empty wifi config list");
            return false;
        }
        for (WifiConfiguration config : wifiConfigurationList) {
            if (fixedSSID.equals(config.SSID.replace("\"", ""))) {
                // do checks here
                Arrays.sort(secureProtocols);
                for (int authType : secureProtocols) {
                    if (config.allowedKeyManagement.get(authType)) {
                        return true;
                    }
                }
                break;
            }
        }
        Log.d(TAG, "no network security check possible with ssid: " + fixedSSID);
        return false;
    }


    /**
     * detect the wifi connection (security) level
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
     * @return
     */
    public String getDNSServerLabel() {
        return context.getString(R.string.nav_header_dns_server_label, this.getDNSServer());
    }

    public String getDNSServer() {
        if (dhcpInfo == null) {
            return "-";
        }
        int dns1 = dhcpInfo.dns1;
        int dns2 = dhcpInfo.dns2;
        String dnsLabel = "";
        if (dns1 != 0) {
            dnsLabel += Converter.intToIp(dns1);
        }
        if (dns2 != 0) {
            dnsLabel += ",\n" + Converter.intToIp(dns2);
        }
        if (dnsLabel.isEmpty()) {
            dnsLabel = "-";
        }
        return dnsLabel;
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

    /**
     * @return
     */
    public String getGatewayLabel() {
        return context.getString(R.string.nav_header_gateway_ip_label, this.getGateway());
    }

    public String getGateway() {
        String gatewayIp = "-";
        if (dhcpInfo != null && dhcpInfo.gateway != 0) {
            gatewayIp = Converter.intToIp(dhcpInfo.gateway);
        }
        return gatewayIp;
    }

    /**
     * method to retrieve ipv6 addresses of wlan0 of this smartphone without loopback
     * and "fe80" prefixed strings. addresses are separated by newlines "\n"
     *
     * @return
     */
    public String getIpv6Addresses() {
        try {
            NetworkInterface wlan0Interface = null;
            for (Enumeration<NetworkInterface> networks = NetworkInterface
                    .getNetworkInterfaces(); networks.hasMoreElements(); ) {
                NetworkInterface netInterface = networks.nextElement();
                if (netInterface.getName().equals("wlan0")) {
                    Log.v(TAG, "found wlan0 interface");
                    wlan0Interface = netInterface;
                    break;
                }
            }
            if (wlan0Interface == null) {
                return "-";
            }
            StringBuilder result = new StringBuilder();
            for (Enumeration<InetAddress> enumIpAddr = wlan0Interface
                    .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
                    Log.d(TAG, "found ipv6 address: " + inetAddress.getHostAddress());
                    if (!inetAddress.getHostAddress().startsWith("fe80")) {
                        if (result.length() != 0) {
                            result.append("\n");
                        }
                        result.append(inetAddress.getHostAddress());
                    }
                }
            }
            if (result.length() != 0) {
                return result.toString();
            }
            return "-";
        } catch (Exception ex) {
            Log.e(TAG, "Error retrieving ipv6 address" + ex.toString());
        }
        return "-";
    }

    public String getIpv6AddressLabel() {
        String ownIpv6 = getIpv6Addresses();
        if (ownIpv6 == null || ownIpv6.isEmpty()) {
            ownIpv6 = "-";
        }
        return context.getString(R.string.drawer_header_ipv6_address_label, ownIpv6);
    }

    /**
     * checks if the fixed ssid network is reachable: its part of wifi scan results
     *
     * @return
     */
    public boolean isTargetNetworkReachable() {
        if (!cockpitPreferenceManager.isWifiSSidLocked()) {
            // we know nothing about a SSID - if the user has not activated this preference
            return false;
        }
        String fixedSSID = cockpitPreferenceManager.getFixedWifiSSID();
        if (fixedSSID == null || fixedSSID.isEmpty() || fixedSSID.equals(WIFI_SSID_AUTO)) {
            return false;
        }
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        if (wifiConfigurationList == null || wifiConfigurationList.isEmpty()) {
            return false;
        }
        for (WifiConfiguration config : wifiConfigurationList) {
            if (fixedSSID.equals(config.SSID.replace("\"", ""))) {
                return true;
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
            return;
        }
        Log.d(TAG, "connect to target network with fixed ssid: " + cockpitPreferenceManager.getFixedWifiSSID());
        String fixedSsid = cockpitPreferenceManager.getFixedWifiSSID();
        if (fixedSsid == null || fixedSsid.isEmpty() || fixedSsid.equals(WIFI_SSID_AUTO)) {
            Log.e(TAG, "connection attempt blocked. empty or invalid ssid.");
            return;
        }

        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        if (wifiConfigurationList == null || wifiConfigurationList.isEmpty()) {
            return;
        }
        for (WifiConfiguration config : wifiConfigurationList) {
            if (fixedSsid.equals(config.SSID.replace("\"", ""))) {
                Log.d(TAG, "trying to set target network to active");

                // notify network stuff to users
                Toast.makeText(context,
                        context.getString(R.string.connect_to_device_toast, fixedSsid), Toast.LENGTH_SHORT).show();
                wifiManager.enableNetwork(config.networkId, true);
                return;
            }
        }

    }
}
