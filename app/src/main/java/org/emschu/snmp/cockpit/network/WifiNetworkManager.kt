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
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import org.emschu.snmp.cockpit.SnmpCockpitApp

/**
 * Singleton class to manage the wifi network related actions of this app:
 * - get network information
 *
 * Everything should work without the required permissions.
 */
object WifiNetworkManager {
    private val TAG = WifiNetworkManager::class.java.name

    private var netInfoProvider: MobileWifiStatusInformation? = null

    private var isCallbackRegistered = false;

    var displayInfo: NetworkDisplayInfo = NetworkDisplayInfo()
        private set

    val displayInfoState: MutableStateFlow<NetworkDisplayInfo> = MutableStateFlow(displayInfo)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(true)
        }

        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(false)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(false)
        }
    }

    init {
        refreshNetworkInfo()
    }

    fun initialize() {
        if (ContextCompat.checkSelfPermission(
                SnmpCockpitApp.context!!, Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!isCallbackRegistered) {
                val applicationContext = SnmpCockpitApp.context!!.applicationContext
                val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                cm.registerNetworkCallback(networkRequest, networkCallback)
                isCallbackRegistered = true
            }
        } else {
            Log.w(TAG, "Missing permissions to register network callback")
        }
    }

    fun refresh() {
        Log.d(TAG, "refreshing dhcp information")
        refreshNetworkInfo()
    }

    private fun refreshNetworkInfo() {
        netInfoProvider = if (ContextCompat.checkSelfPermission(
                SnmpCockpitApp.context!!, Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            WifiStatusInformation()
        } else {
            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(false)
            null
        }
        netInfoProvider?.refresh()
        netInfoProvider?.let {
            displayInfo = NetworkDisplayInfo(it)
        }
        displayInfo.let {
            displayInfoState.tryEmit(it)
        }
    }
}