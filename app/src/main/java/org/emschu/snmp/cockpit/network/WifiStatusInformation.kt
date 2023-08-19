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

package org.emschu.snmp.cockpit.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.context
import org.emschu.snmp.cockpit.util.IpConverter.intToIp
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.SocketException
import kotlin.math.abs

/**
 * implementation of a network information provider used by this app
 */
class WifiStatusInformation : MobileWifiStatusInformation {
    private val cm: ConnectivityManager
    private val wm: WifiManager?
    private var dhcpInfo: DhcpInfo? = null

    init {
        val applicationContext = context!!.applicationContext
        cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        @Suppress("DEPRECATION")
        dhcpInfo = wm.dhcpInfo
    }

    // helper properties
    private var currentSsid: String? = null
    private var currentBssid: String? = null
    private var ipv6AddrsWithSubnetInfo: Array<String> = emptyArray()
    private var ipv4AddrsWithSubnetInfo: Array<String> = emptyArray()
    private var ipv6Addrs: Array<String> = emptyArray()
    private var ipv4Addrs: Array<String> = emptyArray()
    private var lastWifiUpdateAt: Long = 0

    // network is valid and connected
    override val isConnectedToWifiNetwork: Boolean
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        @RequiresApi(Build.VERSION_CODES.Q)
        get() {
            val network = cm.activeNetwork
            if (network != null) {
                val networkCapabilities = cm.getNetworkCapabilities(network)
                if (networkCapabilities != null && (networkCapabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    ) || networkCapabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_ETHERNET
                    ) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                ) {
                    if (wm != null && wm.isWifiEnabled && (wm.wifiState == 2 || wm.wifiState == 3)) {
                        // network is valid and connected
                        if (SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.value == false) {
                            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(true)
                        }
                        return true
                    }
                }
            }
            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(false)
            return false
        }

    // query ssid
    override val sSID: String?
        get() {
            lazyRefresh()
            return currentSsid
        }

    // query bssid
    override val bSSID: String?
        get() {
            lazyRefresh()
            return currentBssid
        }
    override val dNSServers: Array<String>
        get() {
            if (dhcpInfo == null) {
                return emptyArray()
            }
            lazyRefresh()
            val dns1 = dhcpInfo!!.dns1
            val dns2 = dhcpInfo!!.dns2
            val dnsLabel: MutableList<String> = ArrayList()
            if (dns1 != 0) {
                dnsLabel.add(intToIp(dns1))
            }
            if (dns2 != 0) {
                dnsLabel.add(intToIp(dns2))
            }
            return dnsLabel.toTypedArray()
        }

    // query bssid
    override val iPv6Addresses: Array<String>
        get() {
            // query addresses lazy
            lazyRefresh()
            return ipv6AddrsWithSubnetInfo
        }
    override val ipv4AddressesRaw: Array<String>
        get() {
            lazyRefresh()
            return ipv4Addrs
        }
    override val ipv6AddressesRaw: Array<String>
        get() {
            lazyRefresh()
            return ipv6Addrs
        }

    private fun lazyRefresh() {
        if (lastWifiUpdateAt != 0L && abs(lastWifiUpdateAt - System.currentTimeMillis()) < REFRESH_TIMEOUT_MS) {
            return
        }
        update()
    }

    // query bssid
    override val iPv4Addresses: Array<String>
        get() {
            // query bssid
            if (lastWifiUpdateAt != 0L && abs(
                    lastWifiUpdateAt - System.currentTimeMillis()
                ) < REFRESH_TIMEOUT_MS
            ) {
                return ipv4AddrsWithSubnetInfo
            }
            update()
            return ipv4AddrsWithSubnetInfo
        }
    override val gateway: String
        get() {
            var gatewayIp = "-"
            if (dhcpInfo != null && dhcpInfo!!.gateway != 0) {
                gatewayIp = intToIp(dhcpInfo!!.gateway)
            }
            return gatewayIp
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun refresh() {
        if (wm != null) {
            @Suppress("DEPRECATION")
            dhcpInfo = wm.dhcpInfo
        }
        update()
    }

    /* -- private methods for internal logic of this specific implementation -- */
    /**
     * update internal data which is relatively expensive to get (ip addrs, (b)ssid)
     */
    private fun update() {
        lastWifiUpdateAt = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                context!!, Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchWifiInfo()
        }
        fetchIpAddrData()
    }

    /**
     * should be called by #update method only
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun fetchWifiInfo() {
        if (isConnectedToWifiNetwork) {
            val ssid: String
            if (hasPermissions()) {
                if (wm == null) {
                    Log.d(TAG, "WifiManager is null!")
                    currentSsid = null
                    currentBssid = null
                    return
                }

                @Suppress("DEPRECATION")
                val connectionInfo = wm.connectionInfo
                ssid = connectionInfo.ssid
                if (ssid.isBlank()) {
                    currentSsid = null
                    currentBssid = null
                    return
                }
                if (ssid == "internet" || ssid == "<unknown ssid>") {
                    Log.d(TAG, "no wifi network detected!")
                    currentSsid = null
                    currentBssid = null
                    return
                }
                currentBssid = connectionInfo.bssid
                currentSsid = ssid.replace("\"", "")
                return
            } else {
                Log.w(TAG, "Permissions are missing!")
            }
        }
        // fallback
        currentBssid = null
        currentSsid = null
    }

    private fun hasPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        requiredPermissions.forEach {
            if (ContextCompat.checkSelfPermission(context!!, it) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Missing permission '${it}'")
                return false
            }
        }
        return true
    }

    /**
     * should be called for #update method only
     * for v4 and v6
     */
    private fun fetchIpAddrData() {
        try {
            val wlan0Interface = wifiNetworkInterface ?: return
            val showIpv6LinkLocal = SnmpCockpitApp.preferenceManager().isIpv6LinkLocalAddressesDisplayed
            val ipv6AddressListWithSubnetInfo: MutableList<String> = ArrayList()
            val ipv4AddressListWithSubnetInfo: MutableList<String> = ArrayList()
            val ipv6Addresses: MutableList<String> = ArrayList()
            val ipv4Addresses: MutableList<String> = ArrayList()
            for (addr in wlan0Interface.interfaceAddresses) {
                val inetAddress = addr.address
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet6Address) {
                    // respect user's property of displaying ipv6 link local addresses - respects multicast link local addrs also
                    if (!showIpv6LinkLocal && (inetAddress.isLinkLocalAddress() || inetAddress.isMCLinkLocal())) {
                        continue
                    }
                    ipv6AddressListWithSubnetInfo.add(
                        (inetAddress.getHostAddress() ?: "") + " [/" + addr.networkPrefixLength + "]"
                    )
                    ipv6Addresses.add(inetAddress.getHostAddress() ?: "")
                }
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    ipv4AddressListWithSubnetInfo.add(
                        inetAddress.getHostAddress() ?: ("" + " [/" + addr.networkPrefixLength + "]")
                    )
                    ipv4Addresses.add(inetAddress.getHostAddress() ?: "")
                }
            }
            if (ipv6AddressListWithSubnetInfo.size > 0) {
                ipv6AddrsWithSubnetInfo = ipv6AddressListWithSubnetInfo.toTypedArray()
                ipv6Addrs = ipv6Addresses.toTypedArray()
            } else {
                ipv6AddrsWithSubnetInfo = emptyArray()
                ipv6Addrs = emptyArray()
            }
            if (ipv4AddressListWithSubnetInfo.size > 0) {
                ipv4AddrsWithSubnetInfo = ipv4AddressListWithSubnetInfo.toTypedArray()
                ipv4Addrs = ipv4Addresses.toTypedArray()
            } else {
                ipv4AddrsWithSubnetInfo = emptyArray()
                ipv4Addrs = emptyArray()
            }
            return
        } catch (ex: Exception) {
            Log.e(TAG, "Error retrieving ipv4 or v6 address$ex")
        }
        // fallback
        ipv4AddrsWithSubnetInfo = emptyArray()
        ipv4Addrs = emptyArray()
        ipv6AddrsWithSubnetInfo = emptyArray()
        ipv6Addrs = emptyArray()
    }

    @get:Throws(SocketException::class)
    private val wifiNetworkInterface: NetworkInterface?
        get() {
            val networks = NetworkInterface.getNetworkInterfaces()
            while (networks.hasMoreElements()) {
                val netInterface = networks.nextElement()
                if (netInterface.name == "wlan0") {
                    Log.v(TAG, "found wlan0 interface")
                    return netInterface
                }
            }
            return null
        }

    companion object {
        private val TAG = WifiStatusInformation::class.java.simpleName
        const val REFRESH_TIMEOUT_MS = 2500
    }
}