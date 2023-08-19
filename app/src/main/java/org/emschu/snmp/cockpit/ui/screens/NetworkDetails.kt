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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.network.WifiNetworkManager
import org.emschu.snmp.cockpit.ui.theme.PrimaryWarnColor
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel


@Composable
fun NetworkDetailsView(viewModel: MainViewModel) {
    val netState = WifiNetworkManager
    val isLocationEnabled = rememberSaveable { mutableStateOf(SnmpCockpitApp.isLocationEnabled()) }
    val isConnected = SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.observeAsState()
    // this unused variable is important to trigger the ui update mechanism
    @Suppress("UNUSED_VARIABLE")
    val updateTrigger = viewModel.networkDetailsUpdateTrigger.value

    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isConnected.value != true) {
            WarningHint(stringResource(org.emschu.snmp.cockpit.R.string.wifi_network_required_toast))
        }
        if (!isLocationEnabled.value) {
            WarningHint(stringResource(org.emschu.snmp.cockpit.R.string.location_service_required_toast))
        }

        // these labels are rather common and are not translated
        NetworkDetailsInfoRow("SSID", netState.displayInfo.currentSsid)
        NetworkDetailsInfoRow("BSSID", netState.displayInfo.currentBssid)
        NetworkDetailsInfoRow("IP", netState.displayInfo.ipAddress)
        NetworkDetailsInfoRow("DNS", netState.displayInfo.DNSServer)
        NetworkDetailsInfoRow("Gateway", netState.displayInfo.gateway)
        NetworkDetailsInfoRow(
            stringResource(id = org.emschu.snmp.cockpit.R.string.drawer_header_ipv6_address),
            netState.displayInfo.ipv6Addresses
        )
    }
}

@Composable
fun WarningHint(text: String) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.8f)
            .wrapContentHeight(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.padding(end = 12.dp),
                imageVector = Icons.Filled.Warning,
                contentDescription = "warning icon",
                tint = PrimaryWarnColor,
            )
            Text(text, modifier = Modifier.padding(2.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NetworkDetailsInfoRow(
    text: String, value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                text, fontSize = MaterialTheme.typography.h5.fontSize,
                fontWeight = FontWeight.Bold
            )
        }
        Row {
            Text(value, fontSize = MaterialTheme.typography.h6.fontSize)
        }
    }
}