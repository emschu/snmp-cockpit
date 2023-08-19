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

import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp

data class NetworkDisplayInfo(private val statusInfo: MobileWifiStatusInformation? = null) {

    /**
     * raw string ip address of current wifi
     *
     * @return
     */
    val ipAddress: String
        get() {
            if (statusInfo == null) return "-"
            val ipv4Addr = statusInfo.iPv4Addresses
            if (ipv4Addr.isEmpty()) {
                return "-"
            }
            val sb = StringBuilder()
            for (i in ipv4Addr.indices) {
                if (i != 0) {
                    sb.append("\n")
                }
                sb.append(ipv4Addr[i])
            }
            return sb.toString()
        }

    /**
     * get formatted current wifi ip address or null on failure
     *
     * @return
     */
    val ipAddressLabel: String
        get() = String.format(
            SnmpCockpitApp.context!!.resources.getString(R.string.nav_header_ip_label), ipAddress
        )


    /**
     * method to retrieve current ssid we know
     *
     * @return
     */
    val currentSsid: String
        get() {
            if (statusInfo == null) return "-"
            val ssid = statusInfo.sSID
            return if (!ssid.isNullOrBlank()) {
                ssid
            } else "-"
        }

    val currentBssid: String
        get() {
            if (statusInfo == null) return "-"
            val bssid = statusInfo.bSSID
            return if (!bssid.isNullOrBlank()) {
                bssid
            } else "-"
        }

    /**
     * to display ssid or "-" in drawer header
     *
     * @return
     */
    val currentSSIDLabel: String
        get() {
            val currentSsid: String = currentSsid
            return if (currentSsid.isBlank()) {
                SnmpCockpitApp.context!!.getString(R.string.drawer_header_ssid_label, "-")
            } else SnmpCockpitApp.context!!.getString(R.string.drawer_header_ssid_label, currentSsid)
        }

    /**
     * get string of dns server(s)
     *
     * @return
     */
    val DNSServer: String
        get() {
            if (statusInfo == null) return "-"
            val dnsServers = statusInfo.dNSServers
            if (dnsServers.isEmpty()) {
                return "-"
            }
            val sb = StringBuilder()
            for (i in dnsServers.indices) {
                if (i != 0) {
                    sb.append("\n")
                }
                sb.append(dnsServers[i])
            }
            return sb.toString()
        }

    val gateway: String
        get() {
            if (statusInfo == null) return ""
            val gateway = statusInfo.gateway
            return if (gateway.isNullOrBlank()) {
                "-"
            } else gateway
        }

    /**
     * method to retrieve ipv6 addresses of wlan0 of this smartphone without loopback
     * and "fe80" prefixed strings. addresses are separated by newlines "\n"
     *
     * @return
     */
    val ipv6Addresses: String
        get() {
            if (statusInfo == null) return "-"
            val ipv6Addrs = statusInfo.iPv6Addresses
            if (ipv6Addrs.isEmpty()) {
                return "-"
            }
            val sb = StringBuilder()
            for (i in ipv6Addrs.indices) {
                if (i != 0) {
                    sb.append("\n")
                }
                sb.append(ipv6Addrs[i])
            }
            return sb.toString()
        }

    val ipv6AddressLabel: String
        get() {
            val ipv6AddrsLabel = ipv6Addresses
            return SnmpCockpitApp.context!!.getString(R.string.drawer_header_ipv6_address_label, ipv6AddrsLabel)
        }
}