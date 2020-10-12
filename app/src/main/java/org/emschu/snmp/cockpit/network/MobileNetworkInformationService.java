package org.emschu.snmp.cockpit.network;

import androidx.annotation.Nullable;

/**
 * interface of network information
 */
public interface MobileNetworkInformationService {
    // connectivity
    boolean isConnectedToWifiNetwork();

    // ssid
    @Nullable
    String getSSID();
    @Nullable
    String getBSSID();

    // ip addresses (+ network prefix (=subnet) information)
    @Nullable
    String[] getIPv4Addresses();
    @Nullable
    String[] getIPv6Addresses();

    // "raw" address information
    @Nullable
    String[] getIpv4AddressesRaw();
    @Nullable
    String[] getIpv6AddressesRaw();

    // dns
    @Nullable
    String[] getDNSServers();
    @Nullable
    String getGateway();

    // external update trigger
    void refresh();
}
