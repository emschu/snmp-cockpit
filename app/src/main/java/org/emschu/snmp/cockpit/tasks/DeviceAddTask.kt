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

package org.emschu.snmp.cockpit.tasks

import android.content.Context
import android.net.InetAddresses
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.snmpManager
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery
import org.snmp4j.smi.OID

/**
 * Attempt to connect to the provided connection and add the connection to the device list.
 * Executed by Android's WorkManager.
 */
class DeviceAddTask(
    private val context: Context, workerParams: WorkerParameters,
) : Worker(context, workerParams) {
    private val connectionTestTimeout: Int = SnmpCockpitApp.preferenceManager().connectionTestTimeout
    private val connectionTestRetries: Int = SnmpCockpitApp.preferenceManager().connectionTestRetries
    private val connectionTestTotal: Int = snmpManager.totalConnectionTestCount
    private var isTaskStopped: MutableState<Boolean> = mutableStateOf(false)

    companion object {
        private val TAG = DeviceAddTask::class.java.simpleName

        // input data keys
        const val INPUT_DEVICE_CONFIGURATION = "INPUT_DEVICE_CONFIGURATION"

        // progress data keys
        const val PROGRESS_CURRENT_NUM = "PROGRESS_CURRENT"
        const val PROGRESS_ALL_NUM = "PROGRESS_ALL"
        const val PROGRESS_CURRENT_COMBINATION = "CURRENT_COMBINATION"

        // return output data keys
        const val OUTPUT_DOES_EXIST = "DOES_EXIST" // optional
        const val INVALID_CONFIGURATION = "INVALID_CONFIGURATION" // optional
        const val NOT_REACHABLE = "NOT_REACHABLE" // optional
    }

    override fun doWork(): Result {
        TrafficStats.setThreadStatsTag(52323)
        val deviceConfiguration = deviceConfigFromInput(this.inputData)
        setProgressAsync(
            Data.Builder().putInt(PROGRESS_CURRENT_NUM, 0)
                .putString(
                    PROGRESS_CURRENT_COMBINATION,
                    "Init"
                )
                .putInt(PROGRESS_ALL_NUM, 1).build()
        )

        if (isStopped) return Result.success()

        if (deviceConfiguration == null) {
            Log.d(TAG, "No device configuration found!")
            return Result.failure(
                Data.Builder().putBoolean(INVALID_CONFIGURATION, true).build()
            )
        }

        setProgressAsync(
            Data.Builder().putInt(PROGRESS_CURRENT_NUM, 0).putString(
                PROGRESS_CURRENT_COMBINATION,
                context.getString(R.string.connection_to_label)
                    .format(deviceConfiguration.targetIp)
            ).putInt(PROGRESS_ALL_NUM, connectionTestTotal).build()
        )
        // connectivity check - if provided by api
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && !isReachable(deviceConfiguration)) {
            return Result.failure(
                Data.Builder().putBoolean(NOT_REACHABLE, true).build()
            )
        }

        if (snmpManager.doesConnectionExist(deviceConfiguration.uniqueDeviceId)) {
            Log.d(TAG, "Connection does already exist")
            return Result.failure(
                Data.Builder().putBoolean(OUTPUT_DOES_EXIST, true).build()
            )
        }

        setProgressAsync(
            Data.Builder().putInt(PROGRESS_CURRENT_NUM, 0)
                .putString(
                    PROGRESS_CURRENT_COMBINATION,
                    context.getString(R.string.connection_to_label)
                        .format(deviceConfiguration.targetIp)
                )
                .putInt(PROGRESS_ALL_NUM, connectionTestTotal).build()
        )
        // for v3 connections do a (possibly large) connection test to get correct auth and privProtocol
        val deviceConfig: DeviceConfiguration = if (deviceConfiguration.isV3) {
            val testPair = getTestConnection(deviceConfiguration)
            deviceConfiguration.copy(
                authProtocol = testPair.first, privProtocol = testPair.second
            )
        } else {
            deviceConfiguration
        }
        val connector = snmpManager.getOrCreateConnection(deviceConfig)

        // reset testdevice config
        setProgressAsync(
            Data.Builder()
                .putInt(PROGRESS_CURRENT_NUM, 1)
                .putInt(PROGRESS_ALL_NUM, connectionTestTotal)
                .build()
        )
        if (connector == null) {
            Log.w(TAG, "no connection available in device test task")
            return Result.failure()
        }
        
        if (!connector.canPing(deviceConfiguration.copy())) {
            snmpManager.removeConnection(deviceConfiguration)
            connector.close()
            return Result.failure()
        }

        // we need an instance of systemquery in order to show an item
        val systemQueryResult = QueryTaskExecutor.querySnmpDevice(deviceConfiguration, SystemQuery(), false)
        if (systemQueryResult != null) {
            SnmpCockpitApp.deviceManager.add(deviceConfig, systemQueryResult as SystemQuery)
        } else {
            connector.close()
            return Result.failure()
        }
        // connector is not closed, so it will be reused next time it is needed
        return Result.success()
    }

    private fun getTestConnection(usedDeviceConfiguration: DeviceConfiguration): Pair<OID, OID> {
        Log.d(TAG, "start connection check v3")
        val firstWorkingCombination: Pair<OID, OID> = if (usedDeviceConfiguration.isV3) {
            Log.d(TAG, "run connection test")
            val workingSecuritySettings = snmpManager.testConnections(
                usedDeviceConfiguration,
                { counter: Int, currentDesc: String ->
                    setProgressAsync(
                        Data.Builder().putInt(PROGRESS_CURRENT_NUM, counter)
                            .putString(PROGRESS_CURRENT_COMBINATION, currentDesc)
                            .putInt(PROGRESS_ALL_NUM, connectionTestTotal).build()
                    )
                },
                connectionTestTimeout,
                connectionTestRetries,
            )
            if (workingSecuritySettings.isEmpty()) {
                Log.d(TAG, "no auth and privProtocol matched or process was stopped")
                return Pair(null, null)
            }
            workingSecuritySettings[0]
        } else {
            Log.d(TAG, "skip connection test")
            Pair(usedDeviceConfiguration.authProtocol, usedDeviceConfiguration.privProtocol)
        }
        // TODO algorithm to use strongest by default
        Log.d(
            TAG,
            "selected authProtocol: ${firstWorkingCombination.first} and privProtocol: ${firstWorkingCombination.second}"
        )
        return firstWorkingCombination
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun isReachable(deviceConfiguration: DeviceConfiguration): Boolean {
        return InetAddresses.parseNumericAddress(deviceConfiguration.targetIp).isReachable(5000)
    }

    override fun onStopped() {
        super.onStopped()
    }
}