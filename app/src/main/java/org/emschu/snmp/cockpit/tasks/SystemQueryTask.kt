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
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery

/**
 * query all snmp connections for 1.3.6.1.2.1.1 (mib_2.system)
 */
class SystemQueryTask(
    private val context: Context,
    private val workerParams: WorkerParameters,
) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val isCacheAllowed = workerParams.inputData.getBoolean(DATA_KEY_CACHE_ENABLED, false)

        SnmpCockpitApp.deviceManager.deviceConnectionList.forEachIndexed { _, device ->
            SnmpCockpitApp.executorService.execute {
                val systemQuery =
                    QueryTaskExecutor.querySnmpDevice(device.deviceConfiguration, SystemQuery(), isCacheAllowed)
                if (systemQuery == null) {
                    Log.w(this::class.java.simpleName, "no system query retrievable!")
                } else {
                    SnmpCockpitApp.deviceManager.deviceConnectionListMutable.forEach {
                        if (it.id == device.id && it.deviceConfiguration.isOnline) {
                            it.apply {
                                it.systemQuery.value = systemQuery as SystemQuery
                            }
                        }
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        const val DATA_KEY_CACHE_ENABLED = "CACHE_ENABLED"
        const val INPUT_DEVICE_ID = "DEVICE_ID"
    }
}