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

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.*
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.tasks.SystemQueryTask
import org.emschu.snmp.cockpit.ui.components.AppLogo
import org.emschu.snmp.cockpit.ui.components.DeviceCardContent
import org.emschu.snmp.cockpit.ui.theme.accentBackgroundColor
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DeviceListView(navController: NavController, mainViewModel: MainViewModel = viewModel()) {
    val deviceList =
        remember { derivedStateOf { SnmpCockpitApp.deviceManager.deviceConnectionList.sortedBy { it.id } } }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState
    ) {
        if (deviceList.value.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(8.dp)
                        .defaultMinSize(minHeight = 5.dp),
                    shape = RoundedCornerShape(4.dp),
                    backgroundColor = accentBackgroundColor(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 0.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppLogo(modifier = Modifier.width(42.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.no_connections),
                                modifier = Modifier.padding(12.dp),
                                fontSize = MaterialTheme.typography.h6.fontSize,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        } else {
            items(deviceList.value) { device ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .defaultMinSize(minHeight = 5.dp),
                     backgroundColor = accentBackgroundColor(),
                     shape = RoundedCornerShape(8.dp),
                     onClick = {
                         mainViewModel.navigateToDeviceDetails(
                             navController, device.deviceConfiguration.uniqueDeviceId
                         )
                     }) {
                    DeviceCardContent(device)
                }
            }
        }
    }
}

/**
 * Method to refresh all devices at the same time via a work manager job
 */
fun refreshDeviceSystemConnection(context: Context) {
    val inputData = Data.Builder()
        .putBoolean(SystemQueryTask.DATA_KEY_CACHE_ENABLED, false)
        .build()
    val workRequest = OneTimeWorkRequest.Builder(SystemQueryTask::class.java)
        .setInputData(inputData)
        .addTag("Query")
        .build()

    val instance = WorkManager.getInstance(context)
    instance.enqueueUniqueWork("connection_refresh", ExistingWorkPolicy.REPLACE, workRequest)
}
