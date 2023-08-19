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

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.model.DeviceConnection
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.DeviceManager
import org.emschu.snmp.cockpit.snmp.OIDCatalog
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery
import org.emschu.snmp.cockpit.ui.components.anim.DotsTyping
import org.emschu.snmp.cockpit.ui.makeNotification
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetContent(
    viewModel: MainViewModel,
    navController: NavHostController,
    modalBottomSheetState: ModalBottomSheetState,
) {
    val context = LocalContext.current
    val oidQuery by remember {
        derivedStateOf {
            viewModel.bottomSheetData
        }
    }
    val isCardVisible = oidQuery.value.isNotBlank()

    Box(
        Modifier
            .wrapContentHeight(align = Alignment.Bottom)
            .padding(0.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            AnimatedVisibility(
                visible = !isCardVisible, exit = fadeOut(targetAlpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.height(150.dp), contentAlignment = Alignment.Center
                ) {
                    DotsTyping()
                }
            }
            AnimatedVisibility(
                visible = isCardVisible,
                enter = fadeIn(initialAlpha = 0.3f),
            ) {
                Column(
                    Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "OID: ${oidQuery.value}",
                            textAlign = TextAlign.Left,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = MaterialTheme.typography.h5.fontSize,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    val name: String = remember {
                        OIDCatalog.getAsnByOid(oidQuery.value) ?: oidQuery.value
                    }
                    Row {
                        Text(
                            stringResource(R.string.custom_query_name_label), fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Text(name)
                    }
                    val deviceList = SnmpCockpitApp.deviceManager.deviceConnectionList
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (deviceList.isEmpty()) {
                            Text(stringResource(id = R.string.no_connections))
                        } else {
                            Text(stringResource(R.string.please_select_connection))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (deviceList.isNotEmpty()) {
                            ConnectionSelector(
                                deviceList,
                                viewModel,
                                navController,
                                oidQuery.value,
                                modalBottomSheetState,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom
                    ) {
                        val scope = rememberCoroutineScope()
                        Button(onClick = {
                            scope.launch {
                                doIfOnline(context) {
                                    modalBottomSheetState.hide()
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(), content = {
                            Text(stringResource(id = R.string.close))
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ConnectionSelector(
    deviceConnectionList: List<DeviceConnection>,
    viewModel: MainViewModel,
    navController: NavHostController,
    oidQuery: String,
    modalBottomSheetState: ModalBottomSheetState,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // if there is only one connection available - use this immediately
        if (deviceConnectionList.size == 1) {
            LaunchedEffect(key1 = oidQuery, block = {
                viewModel.navigateToQueryDetails(
                    navController, deviceConnectionList[0].deviceConfiguration.uniqueDeviceId, oidQuery
                )
                scope.launch {
                    modalBottomSheetState.hide()
                }
            })
        }

        // >= 2 connections
        LazyColumn(modifier = Modifier.height((4 * 32).dp), contentPadding = PaddingValues(4.dp), content = {
            items(deviceConnectionList) { device ->
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
                DrawerDeviceItemRow(
                    titleLabel = device.deviceConfiguration.getListLabel(null),
                    deviceConfig = device.deviceConfiguration, onDeviceSelected = {
                        viewModel.navigateToQueryDetails(
                            navController, it, oidQuery
                        )
                        scope.launch {
                            modalBottomSheetState.hide()
                        }
                    }, isActive = false
                )
            }
        })
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
private fun BottomSheetViewPreviewLight() {
    SnmpCockpitApp.deviceManager.deviceConnectionListMutable.clear()
    SnmpCockpitApp.deviceManager.add(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1), LocalContext.current)
    SnmpCockpitApp.deviceManager.add(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1), LocalContext.current)
    SnmpCockpitApp.deviceManager.add(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1), LocalContext.current)
    SnmpCockpitApp.deviceManager.add(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1), LocalContext.current)

    CockpitTheme(darkTheme = false) {
        Column {
            val viewModel = MainViewModel()
            viewModel.bottomSheetData.value = "1.2.3.4.5.6"
            BottomSheetContent(
                viewModel = viewModel,
                navController = rememberNavController(),
                rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun ConnectionSelectorLight() {
    val deviceConnectionLists = listOf(
        DeviceConnection(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1),
                         remember { mutableStateOf(SystemQuery()) }),
        DeviceConnection(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1),
                         remember { mutableStateOf(SystemQuery()) }),
        DeviceConnection(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1),
                         remember { mutableStateOf(SystemQuery()) }),
        DeviceConnection(DeviceConfiguration(DeviceConfiguration.SNMP_VERSION.v1),
                         remember { mutableStateOf(SystemQuery()) }),
    )

    CockpitTheme(darkTheme = false) {
        Column {
            val mainViewModel = MainViewModel()
            val oidQuery by remember { mutableStateOf("1.5.6.3.2.1") }
            ConnectionSelector(
                viewModel = mainViewModel,
                navController = rememberNavController(),
                deviceConnectionList = deviceConnectionLists,
                oidQuery = oidQuery,
                modalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
            )
        }
    }
}

suspend inline fun doIfOnline(context: Context, doIfOnline: () -> Unit) {
    if (!DeviceManager.onlineState.value) {
        makeNotification(context, "", context.getString(R.string.no_connection_online))
    } else {
        doIfOnline()
    }
}