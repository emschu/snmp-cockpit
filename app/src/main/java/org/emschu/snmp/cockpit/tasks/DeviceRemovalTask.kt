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
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.emschu.snmp.cockpit.SnmpCockpitApp

class DeviceRemovalTask(
    private val context: Context,
    private val workerParams: WorkerParameters,
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val uniqueDeviceId = workerParams.inputData.getString(INPUT_DEVICE_ID)
        val device = SnmpCockpitApp.deviceManager.getDevice(uniqueDeviceId ?: "")
        if (uniqueDeviceId == null || uniqueDeviceId.isBlank() || device == null || !SnmpCockpitApp.deviceManager.hasDevice(
                uniqueDeviceId
            )
        ) {
            return Result.failure(
                Data.Builder()
                    .putString("msg", "invalid empty or invalid device_id received")
                    .build()
            )
        }
        Log.d("DeviceRemovalTask", "remove item requested: $uniqueDeviceId")
        for (deviceItem in SnmpCockpitApp.deviceManager.deviceConnectionList) {
            if (uniqueDeviceId == deviceItem.deviceConfiguration.uniqueDeviceId) {
                Log.d("DeviceRemovalTask", "item deleted: " + deviceItem.deviceConfiguration.uniqueDeviceId)

                SnmpCockpitApp.deviceManager.deviceConnectionListMutable.remove(deviceItem)
                SnmpCockpitApp.snmpManager.removeConnection(device.deviceConfiguration)

                // clear cache immediately
                SnmpCockpitApp.cockpitStateManager.queryCache.evictDeviceEntries(
                    device.deviceConfiguration.uniqueDeviceId
                )
                break
            }
        }
        return Result.success()
    }

    companion object {
        const val INPUT_DEVICE_ID = "DEVICE_ID"
    }
}