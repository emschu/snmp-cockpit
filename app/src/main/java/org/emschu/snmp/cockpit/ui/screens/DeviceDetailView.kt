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

package org.emschu.snmp.cockpit.ui.screens

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.model.DeviceConnection
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery
import org.emschu.snmp.cockpit.ui.components.*
import org.emschu.snmp.cockpit.ui.sources.CustomQuerySource
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.theme.accentBackgroundColor
import org.emschu.snmp.cockpit.ui.viewmodel.DeviceDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel
import org.emschu.snmp.cockpit.util.createTmpImageAndShare

@Composable
fun DeviceDetailView(
    mainViewModel: MainViewModel,
    navController: NavHostController,
    device: DeviceConnection,
    deviceDetailViewModel: DeviceDetailViewModel = viewModel(),
) {
    val showInfoDialog = rememberSaveable { deviceDetailViewModel.showInfoDialog }
    val showRemoveDeviceDialog = rememberSaveable { deviceDetailViewModel.showRemoveDeviceDialog }
    val showQrCodeDialog = rememberSaveable { deviceDetailViewModel.showQrCodeDialog }
    val context = LocalContext.current

    LaunchedEffect(key1 = "") {
        mainViewModel.updateScreenTitle(device.deviceConfiguration.targetIp)
        deviceDetailViewModel.refreshInformation(context, device)
    }

    Column {
        Row {
            DeviceDetailTabLayout(device, deviceDetailViewModel)
        }
    }

    // handle dialog logic
    if (showInfoDialog.value) {
        AlertDialog(
            onDismissRequest = { showInfoDialog.value = false },
            title = {
                Text(
                    stringResource(R.string.connection_information),
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.h5.fontSize
                )
            }, text = {
                Text(
                    device.deviceConfiguration.connectionDetailsText, fontSize = MaterialTheme.typography.body2.fontSize
                )
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = {
                    showInfoDialog.value = false
                }) {
                    Text(stringResource(R.string.close))
                }
            })
    }

    if (showRemoveDeviceDialog.value) {
        AlertDialog(
            onDismissRequest = { showRemoveDeviceDialog.value = false },
            title = {
                Text(
                    stringResource(R.string.dialog_device_removal_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.h6.fontSize
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRemoveDeviceDialog.value = false
                    SnmpCockpitApp.deviceManager.removeItem(device)
                    mainViewModel.navigateTo(navController, Screen.HOME)
                }) {
                    Text(stringResource(R.string.btn_ok))
                }
            },
            dismissButton = {
                Button(onClick = {
                    showRemoveDeviceDialog.value = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }

    if (showQrCodeDialog.value) {
        AlertDialog(onDismissRequest = { showQrCodeDialog.value = false }, title = {
            Text(
                stringResource(R.string.device_qr_code_label), fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.h5.fontSize
            )
        }, text = {
            // TODO layout/design problem if snmpv3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                val includePassphraseStatus = remember { mutableStateOf(false) }
                val includeEncStatus = remember { mutableStateOf(false) }

                Row {
                    Text(
                        stringResource(R.string.dialog_device_qr_code_sensitive_info_text),
                        fontSize = MaterialTheme.typography.body2.fontSize,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(horizontalArrangement = Arrangement.Center) {
                    ClickableQrCode(
                        device.deviceConfiguration, includePassphraseStatus.value, includeEncStatus.value
                    ) { byteArray ->
                        createTmpImageAndShare(context, byteArray)
                    }
                }

                if (device.deviceConfiguration.isV3) {
                    Row {
                        Column {
                            Switch(checked = includePassphraseStatus.value, onCheckedChange = {
                                includePassphraseStatus.value = it
                            })
                        }
                        Column {
                            Text(stringResource(R.string.qr_code_include_passphrase))
                        }
                    }

                    Row {
                        Column {
                            Switch(checked = includeEncStatus.value, onCheckedChange = {
                                includeEncStatus.value = it
                            })
                        }
                        Column {
                            Text(stringResource(R.string.qr_code_include_enc_key))
                        }
                    }
                }
            }
        }, confirmButton = {}, dismissButton = {
            Button(onClick = {
                showQrCodeDialog.value = false
            }) {
                Text(stringResource(id = R.string.close))
            }
        })
    }
}

fun deviceDetailTabs() = listOfNotNull(
    DeviceTabItem.General,
    DeviceTabItem.Hardware,
    DeviceTabItem.Status,
    if ((CustomQuerySource.tabCustomQueries.value ?: emptyList()).isNotEmpty()) {
        DeviceTabItem.Queries
    } else {
        null
    },
)

@Parcelize
@Stable
sealed class DeviceTabItem(
    val index: Int,
    @StringRes val title: Int,
    @IgnoredOnParcel val screenToLoad: @Composable (viewModel: DeviceDetailViewModel) -> Unit = {},
) : Parcelable {
    // objects
    object General : DeviceTabItem(0, R.string.device_detail_fragment_tab_title, { viewModel ->
        DetailInfoDeviceTab(viewModel)
    })

    object Hardware : DeviceTabItem(1, R.string.device_detail_tab_hardware_label, { viewModel ->
        HardwareDeviceTab(viewModel)
    })

    object Status : DeviceTabItem(2, R.string.device_detail_snmp_usage_query_label, { viewModel ->
        StatusDeviceTab(viewModel)
    })

    object Queries : DeviceTabItem(3, R.string.device_custom_query_tab_title, { viewModel ->
        QueriesDeviceTab(viewModel)
    })

    companion object {
        fun all() = arrayOf(General, Hardware, Status, Queries)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun DeviceDetailTabLayout(
    deviceConnection: DeviceConnection,
    deviceDetailViewModel: DeviceDetailViewModel,
) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxHeight()) {
        Row {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentBackgroundColor())
                    .padding(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(2.dp),
            ) {
                DeviceCardContentShortExpandable(deviceConnection = deviceConnection)
            }
        }
        Tabs(
            deviceDetailTabs(),
            selectedIndex = pagerState.currentPage,
            onPageSelected = { deviceTabItem: DeviceTabItem ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(deviceTabItem.index)
                }
            }
        )

        TabPage(
            deviceTabItems = deviceDetailTabs(),
            pagerState = pagerState,
            deviceDetailViewModel = deviceDetailViewModel,
        )
    }
}

@Composable
fun DetailInfoDeviceTab(
    viewModel: DeviceDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listState = rememberLazyListState()
        val stateList = viewModel.responseDataGeneralTab.observeAsState()
        SnmpQueryResultViewTab(stateList.value, listState)
    }
}

@Composable
fun HardwareDeviceTab(
    viewModel: DeviceDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listState = rememberLazyListState()
        val stateList = viewModel.responseDataHardwareTab.observeAsState()
        SnmpQueryResultViewTab(stateList.value, listState)
    }
}

@Composable
fun QueriesDeviceTab(
    viewModel: DeviceDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listState = rememberLazyListState()
        val stateList = viewModel.responseDataCustomQueriesTab.observeAsState()
        SnmpQueryResultViewTab(stateList.value, listState, false)
    }
}

@Composable
fun StatusDeviceTab(
    viewModel: DeviceDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listState = rememberLazyListState()
        val stateList = viewModel.responseDataStatusTab.observeAsState()
        SnmpQueryResultViewTab(stateList.value, listState)
    }
}

@Preview
@Composable
private fun DeviceDetailPreviewLight() {
    val systemQuery = remember { mutableStateOf(SystemQuery()) }

    CockpitTheme(darkTheme = false) {
        DeviceDetailView(
            mainViewModel = MainViewModel.FACTORY,
            navController = rememberNavController(),
            DeviceConnection(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1), systemQuery)
        )
    }
}

@ExperimentalPagerApi
@Preview
@Composable
private fun DeviceDetailPreviewDark() {
    val systemQuery = remember { mutableStateOf(SystemQuery()) }

    CockpitTheme(darkTheme = true) {
        DeviceDetailView(
            mainViewModel = MainViewModel.FACTORY, navController = rememberNavController(),
            DeviceConnection(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1), systemQuery)
        )
    }
}