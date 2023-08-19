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

package org.emschu.snmp.cockpit.ui.components

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fasterxml.jackson.databind.ObjectMapper
import com.journeyapps.barcodescanner.ScanContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.snmp.SnmpEndpoint
import org.emschu.snmp.cockpit.snmp.json.DeviceQrCode
import org.emschu.snmp.cockpit.ui.makeNotification
import org.emschu.snmp.cockpit.ui.viewmodel.*
import java.io.IOException

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopAppBarLayout(
    mainViewModel: MainViewModel,
    deviceDetailViewModel: DeviceDetailViewModel,
    loginViewModel: LoginViewModel,
    queryDetailViewModel: QueryDetailViewModel,
    queryViewModel: QueryViewModel,
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    isSpinnerActivated: Boolean,
    currentScreen: Screen,
    navController: NavHostController,
) {
    val currentNavFragment = screenToFragment(currentScreen)
    val context = LocalContext.current
    val showMenu = remember { mutableStateOf(false) }

    val qrCodeScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { scanResult ->
        val qrString = scanResult.contents
        if (qrString == null) {
            makeNotification(context, "", R.string.toast_no_valid_device_qr_code)
        } else {
            Log.d("QRCodeScanner", qrString)
            val deviceQrCode: DeviceQrCode? = getDeviceQrCode(qrString)
            if (deviceQrCode != null) {
                val endpoint: SnmpEndpoint? = deviceQrCode.endpoint
                if (endpoint != null) {
                    Log.d(
                        "Qr Code Scanner", "scanned device qr code: $deviceQrCode"
                    )
                    loginViewModel.scannedEndpoint.value = deviceQrCode
                    mainViewModel.navigator.navigateTo(Screen.LOGIN)
                } else {
                    makeNotification(context, "", R.string.invalid_snmp_endpoint_information_in_qr_code_toast)
                }
            } else {
                makeNotification(context, "", R.string.toast_no_valid_device_qr_code)
            }
        }
    }

    TopAppBar(
        title = {
            Row {
                if (singlePageFragments.contains(currentScreen) && mainViewModel.screenTitle.value.isNotBlank()) {
                    // allow single pages to overwrite the screen title value - if they set something
                    Column {
                        Text(mainViewModel.screenTitle.value)
                    }
                } else {
                    Column(modifier = Modifier.wrapContentWidth()) {
                        Text(stringResource(id = currentNavFragment.title))
                    }
                }
            }
        },
        navigationIcon = {
            if (currentScreen == Screen.INFO_SECTION) {
                IconButton(onClick = {
                    scope.launch {
                        mainViewModel.navigateTo(navController, Screen.INFO)
                    }
                }) {
                    Icon(Icons.TwoTone.ArrowBack, "arrow back")
                }
            } else {
                IconButton(onClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }) {
                    Icon(Icons.TwoTone.Menu, "menu")
                }
            }
        },
        backgroundColor = MaterialTheme.colors.primaryVariant,
        contentColor = contentColorFor(MaterialTheme.colors.primarySurface),
        actions = {
            if (isSpinnerActivated) {
                IconButton(onClick = {
                    scope.launch {
                        loginViewModel.cancelConnectionTest(context, mainViewModel)
                    }
                }) {
                    Icon(Icons.TwoTone.Close, "close")
                }
            }

            val currentKeyboard = LocalSoftwareKeyboardController.current
            val isConnected = SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.observeAsState()
            // the following section defines which actions are displayed for a given screen
            when (currentScreen) {
                Screen.LOGIN -> {
                    IconButton(onClick = {
                        scope.launch {
                            loginViewModel.startLoginEvent(qrCodeScannerLauncher, context)
                        }
                    }) {
                        Icon(Icons.TwoTone.QrCodeScanner, "qrcodescanner")
                    }
                    IconButton(
                        onClick = {
                            if (isConnected.value != false) {
                                loginViewModel.login(context, mainViewModel, currentKeyboard)
                            } else {
                                makeNotification(context, "", R.string.wifi_network_required)
                            }
                        }) {
                        Icon(Icons.TwoTone.PlayArrow, "login")
                    }
                }

                Screen.MIBCATALOG -> {
                    IconButton(onClick = {
                        scope.launch {
                            mainViewModel.showMibCatalogImportDialog()
                        }
                    }) {
                        Icon(Icons.TwoTone.Upload, "add mib catalog to app")
                    }
                }

                Screen.NETWORK -> {
                    IconButton(onClick = {
                        scope.launch {
                            mainViewModel.updateNetworkDetails()
                        }
                    }) {
                        Icon(Icons.TwoTone.Refresh, "refresh network button")
                    }
                }

                Screen.HOME -> {
                    if (SnmpCockpitApp.deviceManager.deviceConnectionList.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                mainViewModel.refresh(context)
                            }
                        }) {
                            Icon(Icons.TwoTone.Refresh, "refresh")
                        }
                    }
                }

                Screen.DEVICE_DETAIL -> {
                    IconButton(onClick = {
                        scope.launch {
                            deviceDetailViewModel.refreshInformation(context)
                        }
                    }) {
                        Icon(Icons.TwoTone.Refresh, "refresh")
                    }
                    IconButton(onClick = { showMenu.value = !showMenu.value }) {
                        Icon(Icons.TwoTone.MoreVert, "more options")
                    }
                    DropdownMenu(
                        expanded = showMenu.value,
                        onDismissRequest = { showMenu.value = false },
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(0.dp)
                    ) {
                        DropdownMenuItem(onClick = {
                            showMenu.value = false
                            scope.launch {
                                deviceDetailViewModel.showRemoveDeviceDialog()
                            }
                        }) {
                            Icon(Icons.TwoTone.Delete, "remove device")
                            Text(stringResource(R.string.remove_connection_label), modifier = Modifier.padding(5.dp))
                        }
                        DropdownMenuItem(onClick = {
                            showMenu.value = false
                            scope.launch {
                                deviceDetailViewModel.showQrCodeDialog()
                            }
                        }) {
                            Icon(Icons.TwoTone.QrCode, "device qr code")
                            Text(stringResource(R.string.qr_code_label), modifier = Modifier.padding(5.dp))
                        }
                        DropdownMenuItem(onClick = {
                            showMenu.value = false
                            scope.launch {
                                deviceDetailViewModel.showInfoDialog()
                            }
                        }) {
                            Icon(Icons.TwoTone.Info, "connection information")
                            Text(stringResource(R.string.connection_information), modifier = Modifier.padding(5.dp))
                        }
                    }
                }

                Screen.QUERY_DETAIL -> {
                    IconButton(onClick = {
                        scope.launch {
                            queryDetailViewModel.refreshView()
                        }
                    }) {
                        Icon(Icons.TwoTone.Refresh, "refresh")
                    }
                }

                Screen.QUERIES -> {
                    IconButton(onClick = {
                        scope.launch {
                            queryViewModel.showCreateNewQueryDialog()
                        }
                    }) {
                        Icon(Icons.TwoTone.Add, "add query")
                    }
                }

                else -> {
                    // do nothing = show no action icons by default
                }
            }
        },
    )
}

/**
 * Get the QR-Code
 */
internal fun getDeviceQrCode(qrString: String): DeviceQrCode? {
    try {
        return ObjectMapper()
            .readValue(qrString, DeviceQrCode::class.java)
    } catch (e: IOException) {
        e.printStackTrace()
        Log.e("getDeviceQrCode()", "error reading qr code " + e.message)
    }
    return null
}