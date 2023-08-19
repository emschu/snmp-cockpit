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
import android.os.Parcel
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.work.*
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import org.emschu.snmp.cockpit.BuildConfig
import org.emschu.snmp.cockpit.MainActivity
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.json.DeviceQrCode
import org.emschu.snmp.cockpit.tasks.DeviceAddTask
import org.emschu.snmp.cockpit.ui.components.LoginFormFields
import org.emschu.snmp.cockpit.ui.components.LoginFormValues
import org.emschu.snmp.cockpit.ui.components.ValidatedForm
import org.emschu.snmp.cockpit.ui.components.getLoginFormElements

class LoginViewModel(val state: SavedStateHandle) : ViewModel() {
    private var defaultHost: String = ""
    private var defaultPort: String = ""
    private var defaultCommunity: String = ""
    private var defaultUser: String = ""
    private var defaultPassword: String = ""
    private var defaultKey: String = ""

    init {
        if (BuildConfig.DEBUG) {
            // default local snmp v1 development setup
            defaultHost = "10.0.2.2"
            defaultUser = "snmpuser"
            defaultPassword = "snmpkey3"
            defaultKey = "snmpkey3"
        }
        defaultCommunity = "public"
        defaultPort = "161"
    }

    // form items
    val host: MutableLiveData<String> = MutableLiveData(defaultHost)
    val port: MutableLiveData<String> = MutableLiveData(defaultPort)
    val community: MutableLiveData<String> = MutableLiveData(defaultCommunity)
    val user: MutableLiveData<String> = MutableLiveData(defaultUser)
    val password: MutableLiveData<String> = MutableLiveData(defaultPassword)
    val key: MutableLiveData<String> = MutableLiveData(defaultKey)
    val isIpv6: MutableLiveData<Boolean> = MutableLiveData(false)
    val isSnmpv3: MutableLiveData<Boolean> = MutableLiveData(true)

    // qr code scan results arrive here
    val scannedEndpoint: MutableState<DeviceQrCode?> = mutableStateOf(null)

    val snmpVersion: DeviceConfiguration.SNMP_VERSION
        get() = this.getSnmpVersionToUse(isSnmpv3.value ?: false)

    val validationMapMutable = mutableStateMapOf<String, List<String>>()

    fun populateForm(deviceQrCode: DeviceQrCode) {
        deviceQrCode.endpoint?.let {
            this.isIpv6.postValue(it.isIpv6)
            this.isSnmpv3.postValue(deviceQrCode.isSnmpv3)

            // reset all
            this.host.postValue("")
            this.port.postValue("")
            this.user.postValue("")
            this.community.postValue("")
            this.password.postValue("")
            this.key.postValue("")

            this.host.postValue(it.ipAddress)
            this.port.postValue("" + it.port)
            if (deviceQrCode.isSnmpv3) {
                // v3
                this.user.postValue(deviceQrCode.user)
                this.key.postValue(deviceQrCode.enc)
                this.password.postValue(deviceQrCode.pw)
            } else {
                // v1/v2
                this.community.postValue(deviceQrCode.user)
            }
            // clear this to avoid immediate reset of view
            this.scannedEndpoint.value = null
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun login(
        context: Context, mainViewModel: MainViewModel, currentKeyboard: SoftwareKeyboardController? = null,
    ) {
        currentKeyboard?.hide()

        val loginForm = ValidatedForm(
            getLoginFormElements(
                isSnmpv3.value ?: false, isIpv6.value ?: false
            )
        )

        val formData by lazy {
            mutableMapOf<String, String>(
                Pair(LoginFormFields.HOST_FIELD.key, host.value ?: ""),
                Pair(LoginFormFields.PORT_FIELD.key, port.value ?: ""),
                Pair(LoginFormFields.COMMUNITY_FIELD.key, community.value ?: ""),
                Pair(LoginFormFields.USER_FIELD.key, user.value ?: ""),
                Pair(LoginFormFields.PW_FIELD.key, password.value ?: ""),
                Pair(LoginFormFields.ENC_FIELD.key, key.value ?: ""),
            )
        }
        val valid = loginForm.isValid(context, formData, this.validationMapMutable)
        Log.i("LoginForm", "Connection form validity state: $valid")
        if (!valid) {
            return
        }
        val loginFormData = LoginFormValues(formData, isIpv6.value ?: false, isSnmpv3.value ?: false)

        val dm = SnmpCockpitApp.deviceManager
        val deviceConfiguration = dm.createDeviceConfiguration(
            loginFormData,
            SnmpCockpitApp.preferenceManager().connectionTimeout,
            SnmpCockpitApp.preferenceManager().connectionRetries,
        )

        mainViewModel.startSpinner()

        val parcel = Parcel.obtain()
        deviceConfiguration.writeToParcel(parcel, 0)

        val inputData = Data.Builder()
            .putByteArray(DeviceAddTask.INPUT_DEVICE_CONFIGURATION, parcel.marshall())
            .build()
        val workRequest = OneTimeWorkRequest.Builder(DeviceAddTask::class.java)
            .setInputData(inputData)
            .addTag("LoginTask")
            .build()
        parcel.recycle()

        val instance = WorkManager.getInstance(context)
        instance.enqueueUniqueWork(MainActivity.DEVICE_ADD_TASK_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    fun cancelConnectionTest(context: Context, mainViewModel: MainViewModel) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("LoginTask")

        mainViewModel.endSpinner()
    }

    fun startLoginEvent(
        qrCodeScannerLauncher: ManagedActivityResultLauncher<ScanOptions, ScanIntentResult>,
        context: Context,
    ) {
        val scanOptions = ScanOptions()
        scanOptions.setBarcodeImageEnabled(true)
        scanOptions.setBeepEnabled(false)
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        scanOptions.setOrientationLocked(false)
        scanOptions.setPrompt(context.getString(R.string.scan_device_code_label))
        qrCodeScannerLauncher.launch(scanOptions)
    }

    private fun getSnmpVersionToUse(isSnmpv3: Boolean): DeviceConfiguration.SNMP_VERSION {
        if (isSnmpv3) {
            return DeviceConfiguration.SNMP_VERSION.v3
        }
        if (SnmpCockpitApp.preferenceManager().isV1InsteadOfV2) {
            return DeviceConfiguration.SNMP_VERSION.v1
        }
        return DeviceConfiguration.SNMP_VERSION.v2c
    }
}