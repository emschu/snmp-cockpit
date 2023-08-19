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
import org.emschu.snmp.cockpit.model.QueryRegisterTitledListItem
import org.emschu.snmp.cockpit.model.RegisteredQuery
import org.emschu.snmp.cockpit.model.TabList
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.DeviceManager
import org.emschu.snmp.cockpit.snmp.query.ListQuery
import org.emschu.snmp.cockpit.ui.screens.DeviceTabItem
import org.emschu.snmp.cockpit.ui.sources.SnmpResponseSource
import org.emschu.snmp.cockpit.ui.viewmodel.QueryResponsePair
import org.snmp4j.smi.OID

/**
 * query all snmp connections
 */
class DeviceUpdateTask(
    private val context: Context,
    private val workerParams: WorkerParameters,
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        if (isStopped) return Result.success()
        
        val deviceConfiguration = deviceConfigFromInput(this.inputData)
        if (deviceConfiguration == null) {
            Log.w(TAG, "No device configuration found!")
            return Result.failure(
                Data.Builder().putBoolean(DeviceAddTask.INVALID_CONFIGURATION, true).build()
            )
        }
        val isCacheAllowed = this.inputData.getBoolean(INPUT_CACHE_ALLOWED, false)
        val tabList = tabsFromInput(this.inputData)
        if (tabList == null) {
            Log.w(TAG, "No oid queries found!")
            return Result.failure(
                Data.Builder().putBoolean(DeviceAddTask.INVALID_CONFIGURATION, true).build()
            )
        }
        if (DeviceManager.deviceConnectionList.isEmpty()) {
            // if there are no connections, leave this task
            return Result.success()
        }
        val singleOidQuery: String = this.inputData.getString(INPUT_QUERY) ?: ""

        val executor = SnmpCockpitApp.executorService
        tabList.tabList.forEach { deviceTabItem ->
            executor.execute {
                fetchTab(deviceConfiguration, deviceTabItem, isCacheAllowed, tabList)
            }
        }

        if (singleOidQuery.isNotBlank()) {
            Log.d(TAG, "Query: $singleOidQuery")
            executor.execute {
                SnmpResponseSource.singleQueryData.postValue(listOf())
                val resultList = doQuery(
                    deviceConfiguration, listOf(
                        QueryRegisterTitledListItem(
                            ListQuery(OID(singleOidQuery)), singleOidQuery
                        )
                    ), isCacheAllowed
                )
                SnmpResponseSource.singleQueryData.postValue(resultList)
            }
        }
        return Result.success()
    }

    private fun fetchTab(
        deviceConfiguration: DeviceConfiguration,
        deviceTabItem: DeviceTabItem, isCacheAllowed: Boolean,
        tabList: TabList,
    ) {
        val responseList =
            doQuery(deviceConfiguration, SnmpResponseSource.getQueriesByTab(deviceTabItem), isCacheAllowed)
        if (responseList.isNotEmpty()) {
            val outList = responseList.sortedBy { it.first.query.oidQuery.format() }.toList()
            when (deviceTabItem) {
                is DeviceTabItem.General -> SnmpResponseSource.responseDataGeneralTab.postValue(outList)
                is DeviceTabItem.Status -> SnmpResponseSource.responseDataStatusTab.postValue(outList)
                is DeviceTabItem.Hardware -> SnmpResponseSource.responseDataHardwareTab.postValue(outList)
                is DeviceTabItem.Queries -> SnmpResponseSource.responseDataCustomQueriesTab.postValue(outList)
            }
            Log.d(
                TAG, "Tab update finished with ${tabList.tabList.size} queries and ${responseList.size} responses "
                    + " with ${responseList.sumOf { it.second.size }}"
            )
        }
    }

    private fun doQuery(
        deviceConfiguration: DeviceConfiguration,
        registeredQueries: List<RegisteredQuery>,
        isCacheAllowed: Boolean,
    ): List<QueryResponsePair> {
        val responseList = mutableListOf<QueryResponsePair>()
        registeredQueries.forEach {
            val snmpQueryResult = QueryTaskExecutor.querySnmpDevice(
                deviceConfiguration, it.query, isCacheAllowed
            )
            responseList.add(Pair(it, snmpQueryResult?.responses?.toList() ?: emptyList()))
        }
        return responseList
    }

    companion object {
        private val TAG = DeviceUpdateTask::class.java.simpleName

        // input arguments
        const val INPUT_DEVICE_CONFIGURATION = "INPUT_DEVICE_CONFIGURATION"
        const val INPUT_CACHE_ALLOWED = "INPUT_CACHE_ALLOWED"
        const val INPUT_TABS = "INPUT_TABS"
        const val INPUT_QUERY = "INPUT_QUERY"
    }
}