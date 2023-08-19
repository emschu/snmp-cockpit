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

import android.content.Context
import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.network.WifiNetworkManager
import org.emschu.snmp.cockpit.ui.components.AppNavigator
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.components.singlePageFragments
import org.emschu.snmp.cockpit.ui.screens.InfoScreen
import org.emschu.snmp.cockpit.ui.screens.refreshDeviceSystemConnection

class MainViewModel : ViewModel() {
    val showRemoveDeviceDialog: MutableLiveData<Boolean> = MutableLiveData(false)
    val networkDetailsUpdateTrigger: MutableState<Boolean> = mutableStateOf(false)

    val navigator = AppNavigator()
    val spinner: MutableState<Boolean> = mutableStateOf(false)

    // only read from this outside of this ViewModel! use AppNavigator instead
    val currentScreen: MutableState<Screen> = mutableStateOf(Screen.HOME)
    val currentDeviceScreen: MutableState<String> = mutableStateOf("")

    val screenTitle: MutableState<String> = mutableStateOf("")

    val progressValue: MutableState<Float> = mutableStateOf(0f)
    val progressText: MutableState<String> = mutableStateOf("")

    val bottomSheetData: MutableState<String> = mutableStateOf("")
    val showImportCatalogDialog = mutableStateOf(false)

    fun startSpinner() {
        this.spinner.value = true
    }

    fun endSpinner() {
        this.spinner.value = false
    }

    fun toggleSpinner() {
        this.spinner.value = this.spinner.value.not()
    }

    fun showMibCatalogImportDialog() {
        this.showImportCatalogDialog.value = true
    }

    fun hideMibCatalogImportDialog() {
        this.showImportCatalogDialog.value = false
    }

    /**
     * please avoid to navigate with this method. modify the AppNavigator class!
     */
    fun navigateTo(navController: NavController, screen: Screen) {
        if (screen in singlePageFragments) {
            Log.e("navigateTo", "Wrong usage of navigateTo detected!")
            return
        }
        this.currentDeviceScreen.value = ""

        if (screen == Screen.SETTINGS) {
            this.currentScreen.value = Screen.HOME
        } else {
            this.currentScreen.value = screen

            val to: String = when (screen) {
                Screen.HOME -> "home"
                Screen.MIBCATALOG -> "mibcatalog"
                Screen.LOGIN -> "login"
                Screen.QUERIES -> "queries"
                Screen.NETWORK -> "network"
                Screen.INFO -> "info"

                Screen.INFO_SECTION -> "info_section"
                Screen.DEVICE_DETAIL -> "device_detail"
                Screen.QUERY_DETAIL -> "query_detail"

                Screen.REMOVE_DEVICES_ACTION -> "home"

                else -> "home"
            }
            navController.navigate(to) {
                popUpTo("home") {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    /**
     * use this to navigate to device details section
     */
    fun navigateToDeviceDetails(navController: NavController, deviceId: String) {
        this.navigator.navigateTo(Screen.DEVICE_DETAIL)
        this.currentScreen.value = Screen.DEVICE_DETAIL
        this.currentDeviceScreen.value = deviceId
        navController.navigate("device_detail/" + deviceId)
    }

    /**
     * use this to navigate to info sections
     */
    fun navigateToInfoSection(navController: NavController, id: InfoScreen) {
        this.navigator.navigateTo(Screen.INFO_SECTION)
        this.currentScreen.value = Screen.INFO_SECTION
        this.currentDeviceScreen.value = ""
        navController.navigate("info_section/%s".format(id)) {
            popUpTo("info") {
                saveState = false
            }
        }
    }

    /**
     * use this to navigate to device details section
     */
    fun navigateToQueryDetails(navController: NavController, deviceId: String, oid: String) {
        viewModelScope.launch {
            this@MainViewModel.navigator.navigateTo(Screen.QUERY_DETAIL)
            this@MainViewModel.currentScreen.value = Screen.QUERY_DETAIL
            this@MainViewModel.currentDeviceScreen.value = deviceId
            navController.navigate("query_detail/%s/%s".format(deviceId, oid)) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    /**
     * used in device details section
     */
    fun updateScreenTitle(title: String) {
        this.screenTitle.value = title
    }

    @OptIn(ExperimentalMaterialApi::class)
    fun triggerBottomSheet(scope: CoroutineScope, arg: String, bottomSheetScaffoldState: ModalBottomSheetState) {
        this.bottomSheetData.value = arg
        if (this.bottomSheetData.value.isNotBlank()) {
            scope.launch {
                bottomSheetScaffoldState.show()
            }
        } else {
            scope.launch {
                bottomSheetScaffoldState.hide()
            }
        }
    }

    fun removeAllDevices() {
        CoroutineScope(Dispatchers.IO).launch {
            SnmpCockpitApp.deviceManager.removeAllItems()
        }
    }

    fun refresh(context: Context) {
        refreshDeviceSystemConnection(context)
    }

    fun updateNetworkDetails() {
        WifiNetworkManager.refresh()
        this.networkDetailsUpdateTrigger.value = !this.networkDetailsUpdateTrigger.value
    }

    companion object {
        val FACTORY = ViewModelProvider.NewInstanceFactory()
            .create(MainViewModel::class.java)
    }
}