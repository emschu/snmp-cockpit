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
package org.emschu.snmp.cockpit.snmp

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.snmpManager
import org.emschu.snmp.cockpit.model.DeviceConnection
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery
import org.emschu.snmp.cockpit.tasks.DeviceRemovalTask
import org.emschu.snmp.cockpit.tasks.QueryTaskExecutor
import org.emschu.snmp.cockpit.tasks.SystemQueryTask
import org.emschu.snmp.cockpit.ui.components.LoginFormValues
import org.emschu.snmp.cockpit.ui.makeNotification
import java.util.Collections

/**
 * this singleton class manages devices
 */
object DeviceManager {
    private val managedDevicesList = Collections.synchronizedList(mutableStateListOf<DeviceConnection>())

    val deviceConnectionList: List<DeviceConnection>
        get() = managedDevicesList

    val deviceConnectionListMutable: MutableList<DeviceConnection>
        get() = managedDevicesList

    val onlineState: MutableState<Boolean> = mutableStateOf(true)

    @Synchronized
    fun add(deviceConfiguration: DeviceConfiguration, appContext: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val systemQueryResult = QueryTaskExecutor.querySnmpDevice(deviceConfiguration, SystemQuery(), false)
            CoroutineScope(Dispatchers.Main).launch {
                if (systemQueryResult != null) {
                    SnmpCockpitApp.deviceManager.add(deviceConfiguration, systemQueryResult as SystemQuery)
                    makeNotification(appContext, "", R.string.connection_was_added)
                } else {
                    makeNotification(appContext, R.string.error, R.string.cannot_system_query_oid)
                }
            }
        }
    }

    /**
     * method to add a new snmp connection to the application
     *
     * @param deviceConfiguration
     */
    @Synchronized
    fun add(deviceConfiguration: DeviceConfiguration, systemQuery: SystemQuery) {
        Log.i(TAG, "add " + deviceConfiguration.snmpVersion.toString() + " device to device list.")
        val deviceConnectionItem = DeviceConnection(deviceConfiguration, mutableStateOf(systemQuery))
        Log.d(TAG, "add new device item " + deviceConnectionItem.id)
        managedDevicesList.add(deviceConnectionItem)

        if (SnmpCockpitApp.context != null) {
            val workerData = Data.Builder()
                .putString(SystemQueryTask.INPUT_DEVICE_ID, deviceConfiguration.uniqueDeviceId)
                .putBoolean(SystemQueryTask.DATA_KEY_CACHE_ENABLED, false)
                .build()
            val workRequest = OneTimeWorkRequest.Builder(SystemQueryTask::class.java)
                .setInputData(workerData)
                .addTag("Query")
                .build()
            // allow
            val workManager = WorkManager.getInstance(SnmpCockpitApp.context!!)

            workManager.enqueueUniqueWork(
                "system_query_task_" + deviceConfiguration.uniqueDeviceId,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
        }
    }

    fun createDeviceConfiguration(
        loginFormValues: LoginFormValues, timeout: Int = 0, retries: Int = 0,
    ): DeviceConfiguration {
        val deviceConfig: DeviceConfiguration
        val configurationFactory = SnmpConfigurationFactory()

        // set proper port or fallback to 161
        val portInt: Int = try {
            loginFormValues.port.toInt()
        } catch (nfe: NumberFormatException) {
            Log.w(TAG, "Unexpected invalid target port number in login form! Using 161 as target port")
            161
        }

        deviceConfig = if (loginFormValues.isSnmpv3.not()) {
            configurationFactory.createSnmpV1Config(
                loginFormValues.host,
                community = loginFormValues.community,
                targetPort = portInt,
                timeout = timeout,
                retries = retries,
            )
        } else {
            configurationFactory.createSnmpV3Config(
                targetIp = loginFormValues.host,
                userName = loginFormValues.user,
                password = loginFormValues.authPassphrase,
                encPhrase = loginFormValues.encryptionKey,
                targetPort = portInt,
                isIpv6 = loginFormValues.isIpv6,
                timeout = timeout,
                retries = retries,
            )
        }
        return deviceConfig
    }

    /**
     * method to retrieve a managed device instance by its id
     *
     * @param deviceId
     * @return
     */
    fun getDevice(deviceId: String): DeviceConnection? {
        return this.managedDevicesList.firstOrNull { it.deviceConfiguration.uniqueDeviceId.equals(deviceId) }
    }

    /**
     * method to clear all items. expensive
     */
    @Synchronized
    fun removeAllItems() {
        Log.d(TAG, "remove all devices requested")
        val deviceIdList = managedDevicesList.map { it.deviceConfiguration.uniqueDeviceId }

        for (deviceId in deviceIdList) {
            // remove every single device, but not on its own list
            removeItem(deviceId)
        }
        // should only clean up stuff
        snmpManager.clearConnections()
        managedDevicesList.clear()
    }

    /**
     * remove a single device identified by the deviceId
     *
     * @param uniqueDeviceId
     */
    @Synchronized
    fun removeItem(uniqueDeviceId: String?) {
        requireNotNull(uniqueDeviceId) { "unique device id is null" }
        startItemRemovalWorker(uniqueDeviceId)
    }

    private fun startItemRemovalWorker(uniqueDeviceId: String) {
        val workManager = WorkManager.getInstance(SnmpCockpitApp.context!!)
        val workerData = Data.Builder()
            .putString(DeviceRemovalTask.INPUT_DEVICE_ID, uniqueDeviceId)
            .build()
        val workRequest = OneTimeWorkRequest.Builder(DeviceRemovalTask::class.java)
            .setInputData(workerData)
            .addTag("DeviceRemoval")
            .build()
        workManager.enqueue(workRequest)
    }

    /**
     * @param deviceId
     * @return
     */
    fun hasDevice(deviceId: String): Boolean {
        return managedDevicesList.firstOrNull { it.deviceConfiguration.uniqueDeviceId == deviceId } != null
    }

    /**
     * Method to check if there is an open snmp v2c connection
     *
     * @return
     */
    fun hasV2Connection(): Boolean = managedDevicesList.any { it.deviceConfiguration.isV2c }

    /**
     * Method to check if there is an open snmp v1 connection
     *
     * @return
     */
    fun hasV1Connection(): Boolean = managedDevicesList.any { it.deviceConfiguration.isV1 }

    fun removeItem(device: DeviceConnection) {
        this.removeItem(device.deviceConfiguration.uniqueDeviceId)
        onlineState.value = deviceConnectionList.all { it.deviceConfiguration.isOnline }
    }

    fun registerUnreachable(deviceConfiguration: DeviceConfiguration) {
        managedDevicesList.firstOrNull { it.deviceConfiguration.uniqueDeviceId == deviceConfiguration.uniqueDeviceId }
            ?.also {
                if (it.deviceConfiguration.isOnline) {
                    it.deviceConfiguration = deviceConfiguration.copy(isOnline = false)
                }
            }
        onlineState.value = deviceConnectionList.all { it.deviceConfiguration.isOnline }
    }

    fun registerReachable(deviceConfiguration: DeviceConfiguration) {
        managedDevicesList.firstOrNull { it.deviceConfiguration.uniqueDeviceId == deviceConfiguration.uniqueDeviceId }
            ?.also {
                if (!it.deviceConfiguration.isOnline) {
                    it.deviceConfiguration = deviceConfiguration.copy(isOnline = true)
                }
            }
        onlineState.value = deviceConnectionList.all { it.deviceConfiguration.isOnline }
    }

    private val TAG = DeviceManager::class.java.name
}