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

package org.emschu.snmp.cockpit.ui.viewmodel

import android.app.Application
import android.os.Parcel
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.MainActivity
import org.emschu.snmp.cockpit.model.DeviceConnection
import org.emschu.snmp.cockpit.tasks.DeviceUpdateTask
import org.emschu.snmp.cockpit.ui.sources.SnmpResponseSource


class QueryDetailViewModel(application: Application) : AndroidViewModel(application) {

    val responseContent: LiveData<QueryResponseCollection> = SnmpResponseSource.singleQueryData.distinctUntilChanged()

    private val currentQuery: MutableState<String?> = mutableStateOf(null)
    private val currentDevice: MutableState<DeviceConnection?> = mutableStateOf(null)
    private val workManager = WorkManager.getInstance(application.applicationContext)

    fun refreshView() {
        if (currentDevice.value == null || currentQuery.value.isNullOrEmpty()) {
            return
        }
        val deviceConnection = currentDevice.value!!
        val currentQuery = currentQuery.value

        CoroutineScope(Dispatchers.IO).launch {
            val deviceConfigParcel = Parcel.obtain()
            deviceConnection.deviceConfiguration.writeToParcel(deviceConfigParcel, 0)

            val inputData = Data.Builder()
                .putByteArray(DeviceUpdateTask.INPUT_DEVICE_CONFIGURATION, deviceConfigParcel.marshall())
                .putByteArray(DeviceUpdateTask.INPUT_TABS, byteArrayOf())
                .putString(DeviceUpdateTask.INPUT_QUERY, currentQuery)
                .putBoolean(DeviceUpdateTask.INPUT_CACHE_ALLOWED, false)
                .build()
            val workRequest = OneTimeWorkRequest.Builder(DeviceUpdateTask::class.java)
                .setInputData(inputData)
                .addTag(MainActivity.DEVICE_UPDATE_TASK_WORK_NAME)
                .build()
            deviceConfigParcel.recycle()

            workManager.enqueue(workRequest)
        }
    }

    fun addCurrentQuery(query: String) {
        this.currentQuery.value = query
    }

    fun addLatestDevice(device: DeviceConnection) {
        this.currentDevice.value = device
    }
}