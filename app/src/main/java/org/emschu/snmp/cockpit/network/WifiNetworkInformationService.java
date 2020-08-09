package org.emschu.snmp.cockpit.network;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.util.Converter;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * implementation of a network information provider used by this app
 */
public class WifiNetworkInformationService implements MobileNetworkInformationService {
    private final static String TAG = WifiNetworkInformationService.class.getSimpleName();
    public static final int REFRESH_TIMEOUT_MS = 2500;
    // services
    private final ConnectivityManager cm;
    private final WifiManager wm;
    private DhcpInfo dhcpInfo;
    // helper properties
    private String currentSsid;
    private String currentBssid;
    private String[] ipv6AddrsWithSubnetInfo;
    private String[] ipv4AddrsWithSubnetInfo;
    private String[] ipv6Addrs;
    private String[] ipv4Addrs;

    private long lastWifiUpdateAt;

    /**
     * constructor
     */
    public WifiNetworkInformationService() {
        Context applicationContext = SnmpCockpitApp.getContext().getApplicationContext();
        this.cm = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wm = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        if (this.wm != null) {
            this.dhcpInfo = this.wm.getDhcpInfo();
        }
    }

    @Override
    public boolean isConnectedToWifiNetwork() {
        Network network = this.cm.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities networkCapabilities = this.cm.getNetworkCapabilities(network);
            if (networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))) {
                if (this.wm != null && this.wm.isWifiEnabled() && (this.wm.getWifiState() == 2 || this.wm.getWifiState() == 3)) {
                    // network is valid and connected
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getSSID() {
        // query ssid
        if (lastWifiUpdateAt != 0 && Math.abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return this.currentSsid;
        }
        this.update();
        return this.currentSsid;
    }

    @Nullable
    @Override
    public String getBSSID() {
        // query bssid
        if (lastWifiUpdateAt != 0 && Math.abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return this.currentBssid;
        }
        this.update();
        return this.currentBssid;
    }

    @Nullable
    @Override
    public String[] getDNSServers() {
        if (this.dhcpInfo == null) {
            return null;
        }
        int dns1 = dhcpInfo.dns1;
        int dns2 = dhcpInfo.dns2;
        List<String> dnsLabel = new ArrayList<>();
        if (dns1 != 0) {
            dnsLabel.add(Converter.intToIp(dns1));
        }
        if (dns2 != 0) {
            dnsLabel.add(Converter.intToIp(dns2));
        }
        return dnsLabel.toArray(new String[0]);
    }

    @Nullable
    @Override
    public String[] getIPv6Addresses() {
        // query bssid
        if (lastWifiUpdateAt != 0 && Math.abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return this.ipv6AddrsWithSubnetInfo;
        }
        this.update();
        return this.ipv6AddrsWithSubnetInfo;
    }

    @Nullable
    @Override
    public String[] getIpv4AddressesRaw() {
        if (lastWifiUpdateAt != 0 && Math.abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return this.ipv4Addrs;
        }
        this.update();
        return this.ipv4Addrs;
    }

    @Nullable
    @Override
    public String[] getIpv6AddressesRaw() {
        if (lastWifiUpdateAt != 0 && Math.abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return this.ipv6Addrs;
        }
        this.update();
        return this.ipv6Addrs;
    }

    @Nullable
    @Override
    public String[] getIPv4Addresses() {
        // query bssid
        if (lastWifiUpdateAt != 0 && Math.abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return this.ipv4AddrsWithSubnetInfo;
        }
        this.update();
        return this.ipv4AddrsWithSubnetInfo;
    }

    @Nullable
    @Override
    public String getGateway() {
        String gatewayIp = "-";
        if (dhcpInfo != null && dhcpInfo.gateway != 0) {
            gatewayIp = Converter.intToIp(dhcpInfo.gateway);
        }
        return gatewayIp;
    }

    @Override
    public void refresh() {
        if (this.wm != null) {
            this.dhcpInfo = this.wm.getDhcpInfo();
        }
        this.update();
    }

    /* -- private methods for internal logic of this specific implementation -- */

    /**
     * update internal data which is relatively expensive to get (ip addrs, (b)ssid)
     */
    private void update() {
        this.lastWifiUpdateAt = System.currentTimeMillis();
        this.fetchWifiInfo();
        this.fetchIpAddrData();
    }

    /**
     * should be called by #update method only
     */
    private void fetchWifiInfo() {
        if (this.isConnectedToWifiNetwork()) {
            if (this.wm != null) {
                String ssid;
                if (ContextCompat.checkSelfPermission(SnmpCockpitApp.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    ssid = connectionInfo.getSSID();
                    Log.v(TAG, "WiFi SSID of android network info: " + ssid);
                    if (ssid == null || ssid.trim().isEmpty()) {
                        this.currentSsid = null;
                        this.currentBssid = null;
                        return;
                    }
                    if (ssid.equals("internet") || ssid.equals("<unknown ssid>")) {
                        Log.d(TAG, "no wifi network detected!");
                        this.currentSsid = null;
                        this.currentBssid = null;
                        return;
                    }
                    this.currentBssid = connectionInfo.getBSSID();
                    this.currentSsid = ssid.replace("\"", "");
                    return;
                } else {
                    Log.e(TAG, "Permissions are missing!");
                }
            }
        }
        // fallback
        this.currentBssid = null;
        this.currentSsid = null;
    }

    /**
     * should be called for #update method only
     * for v4 and v6
     */
    private void fetchIpAddrData() {
        try {
            NetworkInterface wlan0Interface = getWifiNetworkInterface();
            if (wlan0Interface == null) {
                return;
            }
            final boolean showIpv6LinkLocal = SnmpCockpitApp.getPreferenceManager().isIpv6LinkLocalAddressesDisplayed();

            List<String> ipv6AddressListWithSubnetInfo = new ArrayList<>();
            List<String> ipv4AddressListWithSubnetInfo = new ArrayList<>();
            List<String> ipv6Addresses = new ArrayList<>();
            List<String> ipv4Addresses = new ArrayList<>();
            for (InterfaceAddress addr : wlan0Interface.getInterfaceAddresses()) {
                InetAddress inetAddress = addr.getAddress();

                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
                    Log.d(TAG, "found ipv6 address: " + inetAddress.getHostAddress());
                    // respect user's property of displaying ipv6 link local addresses - respects multicast link local addrs also
                    if (showIpv6LinkLocal && (inetAddress.isLinkLocalAddress() || inetAddress.isMCLinkLocal())) {
                        ipv6AddressListWithSubnetInfo.add(inetAddress.getHostAddress() + " [/" + addr.getNetworkPrefixLength() + "]");
                        ipv6Addresses.add(inetAddress.getHostAddress());
                    }
                    if (!showIpv6LinkLocal && !inetAddress.isLinkLocalAddress() && !inetAddress.isMCLinkLocal()) {
                        ipv6AddressListWithSubnetInfo.add(inetAddress.getHostAddress() + " [/" + addr.getNetworkPrefixLength() + "]");
                        ipv6Addresses.add(inetAddress.getHostAddress());
                    }
                }
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    Log.d(TAG, "found v4 address: " + inetAddress.getHostAddress());
                    ipv4AddressListWithSubnetInfo.add(inetAddress.getHostAddress() + " [/" + addr.getNetworkPrefixLength() + "]");
                    ipv4Addresses.add(inetAddress.getHostAddress());
                }
            }

            if (ipv6AddressListWithSubnetInfo.size() > 0) {
                this.ipv6AddrsWithSubnetInfo = ipv6AddressListWithSubnetInfo.toArray(new String[0]);
                this.ipv6Addrs = ipv6Addresses.toArray(new String[0]);
            } else {
                this.ipv6AddrsWithSubnetInfo = null;
                this.ipv6Addrs = null;
            }
            if (ipv4AddressListWithSubnetInfo.size() > 0) {
                this.ipv4AddrsWithSubnetInfo = ipv4AddressListWithSubnetInfo.toArray(new String[0]);
                this.ipv4Addrs = ipv4Addresses.toArray(new String[0]);
            } else {
                this.ipv4AddrsWithSubnetInfo = null;
                this.ipv4Addrs = null;
            }
            return;
        } catch (Exception ex) {
            Log.e(TAG, "Error retrieving ipv4 or v6 address" + ex.toString());
        }
        // fallback
        this.ipv4AddrsWithSubnetInfo = null;
        this.ipv4Addrs = null;
        this.ipv6AddrsWithSubnetInfo = null;
        this.ipv6Addrs = null;
    }

    @Nullable
    private NetworkInterface getWifiNetworkInterface() throws SocketException {
        for (Enumeration<NetworkInterface> networks = NetworkInterface
                .getNetworkInterfaces(); networks.hasMoreElements(); ) {
            NetworkInterface netInterface = networks.nextElement();
            if (netInterface.getName().equals("wlan0")) {
                Log.v(TAG, "found wlan0 interface");
                return netInterface;
            }
        }
        return null;
    }
}
