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
import android.content.Context
import android.os.Parcel
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.MainActivity
import org.emschu.snmp.cockpit.model.DeviceConnection
import org.emschu.snmp.cockpit.model.RegisteredQuery
import org.emschu.snmp.cockpit.model.TabList
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.emschu.snmp.cockpit.tasks.DeviceUpdateTask
import org.emschu.snmp.cockpit.ui.screens.DeviceTabItem
import org.emschu.snmp.cockpit.ui.screens.deviceDetailTabs
import org.emschu.snmp.cockpit.ui.screens.refreshDeviceSystemConnection
import org.emschu.snmp.cockpit.ui.sources.CustomQuerySource
import org.emschu.snmp.cockpit.ui.sources.SnmpResponseSource

typealias QueryResponsePair = Pair<RegisteredQuery, List<QueryResponse>>
typealias QueryResponseCollection = List<QueryResponsePair>

class DeviceDetailViewModel(application: Application, val state: SavedStateHandle) : AndroidViewModel(application) {
    val showInfoDialog = mutableStateOf(false)
    val showRemoveDeviceDialog = mutableStateOf(false)
    val showQrCodeDialog = mutableStateOf(false)

    private val workManager = WorkManager.getInstance(application.applicationContext)

    private val currentDevice: MutableState<DeviceConnection?> = mutableStateOf(null)

    val responseDataGeneralTab: LiveData<QueryResponseCollection> =
        SnmpResponseSource.responseDataGeneralTab.distinctUntilChanged()
    val responseDataHardwareTab: LiveData<QueryResponseCollection> =
        SnmpResponseSource.responseDataHardwareTab.distinctUntilChanged()
    val responseDataStatusTab: LiveData<QueryResponseCollection> =
        SnmpResponseSource.responseDataStatusTab.distinctUntilChanged()
    val responseDataCustomQueriesTab: LiveData<QueryResponseCollection> =
        SnmpResponseSource.responseDataCustomQueriesTab.distinctUntilChanged()

    fun showInfoDialog() {
        this.showInfoDialog.value = true
    }

    fun hideInfoDialog() {
        this.showInfoDialog.value = false
    }

    fun showRemoveDeviceDialog() {
        this.showRemoveDeviceDialog.value = true
    }

    fun hideRemoveDeviceDialog() {
        this.showRemoveDeviceDialog.value = false
    }

    fun showQrCodeDialog() {
        this.showQrCodeDialog.value = true
    }

    fun hideQrCodeDialog() {
        this.showQrCodeDialog.value = false
    }

    fun refreshInformation(context: Context, device: DeviceConnection? = null) {
        refreshDeviceSystemConnection(context)

        if (device != null) {
            this.triggerReload(device.deviceConfiguration, false)
        } else if (this.currentDevice.value != null) {
            this.triggerReload(this.currentDevice.value!!.deviceConfiguration, false)
        }

        viewModelScope.launch {
            CustomQuerySource.refresh()
        }
    }

    fun addLatestDevice(device: DeviceConnection) {
        this.currentDevice.value = device
    }

    private fun triggerReload(
        deviceConfiguration: DeviceConfiguration,
        isCacheAllowed: Boolean,
        tabs: List<DeviceTabItem> = deviceDetailTabs(),
    ) {
        val deviceConfigParcel = Parcel.obtain()
        val queryParcel = Parcel.obtain()
        deviceConfiguration.writeToParcel(deviceConfigParcel, 0)

        val queries = TabList(tabs)
        queries.writeToParcel(queryParcel, 0)

        val inputData = Data.Builder()
            .putByteArray(DeviceUpdateTask.INPUT_DEVICE_CONFIGURATION, deviceConfigParcel.marshall())
            .putByteArray(DeviceUpdateTask.INPUT_TABS, queryParcel.marshall())
            .putBoolean(DeviceUpdateTask.INPUT_CACHE_ALLOWED, isCacheAllowed)
            .build()
        val workRequest = OneTimeWorkRequest.Builder(DeviceUpdateTask::class.java)
            .setInputData(inputData)
            .addTag(MainActivity.DEVICE_UPDATE_TASK_WORK_NAME)
            .build()
        deviceConfigParcel.recycle()
        queryParcel.recycle()

        workManager.beginWith(workRequest).enqueue()
    }
}