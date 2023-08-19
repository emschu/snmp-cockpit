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

/**
 * interface of network information
 */
sealed interface MobileWifiStatusInformation {
    // connectivity
    val isConnectedToWifiNetwork: Boolean

    // ssid
    val sSID: String?
    val bSSID: String?

    // ip addresses (+ network prefix (=subnet) information)
    val iPv4Addresses: Array<String>
    val iPv6Addresses: Array<String>

    // "raw" address information
    val ipv4AddressesRaw: Array<String>
    val ipv6AddressesRaw: Array<String>

    // dns
    val dNSServers: Array<String>
    val gateway: String?

    // external update trigger
    fun refresh()
}