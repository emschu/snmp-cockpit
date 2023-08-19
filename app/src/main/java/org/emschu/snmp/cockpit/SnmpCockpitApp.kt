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
package org.emschu.snmp.cockpit

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper
import org.emschu.snmp.cockpit.snmp.DeviceManager
import org.emschu.snmp.cockpit.snmp.SnmpManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * simple application class which provides the context within the app
 */
class SnmpCockpitApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        context = applicationContext
        preferenceManager = CockpitPreferenceManager(context!!)
        dbHelper = CockpitDbHelper(context!!)
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
        .build()

    companion object {
        @JvmStatic
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
            private set

        @JvmStatic
        @SuppressLint("StaticFieldLeak")
        var dbHelper: CockpitDbHelper? = null
            private set

        @JvmStatic
        var preferenceManager: CockpitPreferenceManager? = null
            private set

        fun preferenceManager(): CockpitPreferenceManager = preferenceManager!!

        fun dbHelper(): CockpitDbHelper = dbHelper!!

        val cockpitStateManager: CockpitStateManager = CockpitStateManager

        val deviceManager: DeviceManager = DeviceManager

        val snmpManager: SnmpManager = SnmpManager

        val executorService: ExecutorService = Executors.newFixedThreadPool(4)

        fun isLocationEnabled(): Boolean = if (Build.VERSION.SDK_INT >= 29) {
            val systemService = context?.getSystemService(LOCATION_SERVICE) as LocationManager?
            systemService?.isLocationEnabled ?: false
        } else {
            true
        }

        fun isConnectedToWifi(): Boolean = cockpitStateManager.networkAvailabilityObservable.value ?: true
    }
}