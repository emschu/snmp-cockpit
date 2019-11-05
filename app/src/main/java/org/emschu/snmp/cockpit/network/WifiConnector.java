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

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.zxing.client.result.WifiParsedResult;

import java.util.Arrays;

import org.emschu.snmp.cockpit.util.Converter;

/**
 * class to change current wifi connection
 *
 * inspiration: https://github.com/zxing/zxing/blob/master/android/src/com/google/zxing/client/android/wifi/WifiConfigManager.java
 *
 * TODO implement more wifi connection types, not only wpa2 and wpa2-eap?
 */
public class WifiConnector {
    private static final String TAG = WifiConnector.class.getName();
    public static final String WPA2_KEY = "WPA2";
    public static final String WPA2_EAP_KEY = "WPA2-EAP";

    private static final String[] allowedWifiTypes = {WPA2_KEY, WPA2_EAP_KEY};
    private WifiParsedResult parsedWifiResult;
    private String networkType = null;
    private boolean canConnect = false;
    protected boolean isFailed = false;

    /**
     *
     * @param parsedWifiResult
     */
    public WifiConnector(WifiParsedResult parsedWifiResult) {
        this.parsedWifiResult = parsedWifiResult;
        networkType = parsedWifiResult.getNetworkEncryption();
        if (Arrays.asList(allowedWifiTypes).contains(networkType)) {
            Log.d(TAG, "Detected supported wifi type: " + networkType);
            // we can connect!
            canConnect = true;
        } else {
            Log.d(TAG, "Detected unsupported wifi type: " + networkType);
        }
    }

    /**
     * call this before you call {@link #connect(WifiManager)}
     *
     * @return
     */
    public boolean canConnect() {
        return canConnect;
    }

    /**
     * connect to a new network
     * call {@link #canConnect} before!
     *
     * @param wifiManager
     */
    public void connect(WifiManager wifiManager) {
        if (!canConnect) {
            throw new IllegalStateException("not allowed to run! unsupported network type: " + networkType);
        }

        switch (networkType) {
            case WPA2_KEY:
                connectToWpa2Network(wifiManager, parsedWifiResult);
                break;
            case WPA2_EAP_KEY:
                connectToWpa2EAPNetwork(wifiManager, parsedWifiResult);
                break;
            default:
                throw new IllegalStateException("unsupported network type: " + networkType);
        }
    }

    // Adding a WPA or WPA2 network
    private void connectToWpa2Network(WifiManager wifiManager, WifiParsedResult wifiResult) {
        WifiConfiguration config = initWifiConfiguration(wifiResult);
        config.preSharedKey = Converter.quoteNonHex(wifiResult.getPassword(), 64);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        updateNetwork(wifiManager, config);
    }

    private void connectToWpa2EAPNetwork(WifiManager wifiManager, WifiParsedResult wifiResult) {
        WifiConfiguration config = initWifiConfiguration(wifiResult);
        // Hex passwords that are 64 bits long are not to be quoted.
        config.preSharedKey = Converter.quoteNonHex(wifiResult.getPassword(), 64);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.enterpriseConfig.setIdentity(wifiResult.getIdentity());
        config.enterpriseConfig.setAnonymousIdentity(wifiResult.getAnonymousIdentity());
        config.enterpriseConfig.setPassword(wifiResult.getPassword());
        config.enterpriseConfig.setEapMethod(parseEap(wifiResult.getEapMethod()));
        config.enterpriseConfig.setPhase2Method(parsePhase2(wifiResult.getPhase2Method()));
        updateNetwork(wifiManager, config);
    }

    private WifiConfiguration initWifiConfiguration(WifiParsedResult wifiResult) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = Converter.quoteNonHex(wifiResult.getSsid());
        config.hiddenSSID = wifiResult.isHidden();
        return config;
    }

    private int parseEap(String eapString) {
        if (eapString == null) {
            return WifiEnterpriseConfig.Eap.NONE;
        }
        switch (eapString) {
            case "NONE":
                return WifiEnterpriseConfig.Eap.NONE;
            case "PEAP":
                return WifiEnterpriseConfig.Eap.PEAP;
            case "PWD":
                return WifiEnterpriseConfig.Eap.PWD;
            case "TLS":
                return WifiEnterpriseConfig.Eap.TLS;
            case "TTLS":
                return WifiEnterpriseConfig.Eap.TTLS;
            default:
                throw new IllegalArgumentException("Unknown EAP value: " + eapString);
        }
    }

    private int parsePhase2(String phase2String) {
        if (phase2String == null) {
            return WifiEnterpriseConfig.Phase2.NONE;
        }
        switch (phase2String) {
            case "GTC":
                return WifiEnterpriseConfig.Phase2.GTC;
            case "MSCHAP":
                return WifiEnterpriseConfig.Phase2.MSCHAP;
            case "MSCHAPV2":
                return WifiEnterpriseConfig.Phase2.MSCHAPV2;
            case "NONE":
                return WifiEnterpriseConfig.Phase2.NONE;
            case "PAP":
                return WifiEnterpriseConfig.Phase2.PAP;
            default:
                throw new IllegalArgumentException("Unknown phase 2 value: " + phase2String);
        }
    }

    /**
     * Updating network
     *
     * @param wifiManager
     * @param config
     */
    private void updateNetwork(WifiManager wifiManager, WifiConfiguration config) {
        Integer foundNetworkID = findNetworkInExistingConfig(wifiManager, config.SSID);
        if (foundNetworkID != null) {
            Log.i(TAG, "Removing old configuration for network " + config.SSID);
            wifiManager.removeNetwork(foundNetworkID);
        }
        int networkId = wifiManager.addNetwork(config);
        if (networkId >= 0) {
            if (wifiManager.enableNetwork(networkId, true)) {
                Log.i(TAG, "Associating to network " + config.SSID);
                isFailed = false;
            } else {
                Log.w(TAG, "Failed to enable network " + config.SSID);
                isFailed = true;
            }
        } else {
            Log.w(TAG, "Unable to add network " + config.SSID);
        }
    }

    /**
     * check if a network config exists
     *
     * @param wifiManager
     * @param ssid
     * @return
     */
    private Integer findNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
        Iterable<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                String existingSSID = existingConfig.SSID;
                if (existingSSID != null && existingSSID.equals(ssid)) {
                    return existingConfig.networkId;
                }
            }
        }
        return null;
    }


    public boolean isFailed() {
        return isFailed;
    }
}
