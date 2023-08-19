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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.model.DeviceConnection
import org.emschu.snmp.cockpit.ui.theme.accentBackgroundColor

@Composable
fun DeviceCardContent(deviceConnection: DeviceConnection) {
    // only relevant for short mode
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(accentBackgroundColor()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val systemQuery = deviceConnection.systemQuery.value

        val systemStr = "%s (SNMP %s)".format(systemQuery.sysName, deviceConnection.deviceConfiguration.snmpVersion)
        val connectionString = deviceConnection.deviceConfiguration.getListLabel(null)

        DeviceCardRow(stringResource(R.string.connection_label), "$systemStr: $connectionString")
        DeviceCardRow("sysUptime", systemQuery.sysUpTime)
        DeviceCardRow("sysDescr", systemQuery.sysDescr)
        DeviceCardRow("sysLocation", systemQuery.sysLocation)
        DeviceCardRow("sysContact", systemQuery.sysContact)
        DeviceCardRow("sysObjectId", systemQuery.sysObjectId)
        DeviceCardRow("sysServices", systemQuery.sysServices)
    }
}


@Composable
fun DeviceCardContentShortExpandable(deviceConnection: DeviceConnection) {
    // only relevant for short mode
    val showMore = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                showMore.value = !showMore.value
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val systemQuery = deviceConnection.systemQuery.value

        val systemStr = "%s (SNMP %s)".format(systemQuery.sysName, deviceConnection.deviceConfiguration.snmpVersion)
        val connectionString = deviceConnection.deviceConfiguration.getListLabel(null)

        DeviceCardRow(stringResource(id = R.string.connection_label), "$systemStr: $connectionString")
        DeviceCardRow("sysUptime", systemQuery.sysUpTime)
        if (showMore.value) {
            DeviceCardRow("sysDescr", systemQuery.sysDescr)
            DeviceCardRow("sysLocation", systemQuery.sysLocation)
            DeviceCardRow("sysContact", systemQuery.sysContact)
            DeviceCardRow("sysObjectId", systemQuery.sysObjectId)
            DeviceCardRow("sysServices", systemQuery.sysServices)
        }
    }
}

@Composable
fun NotOnlineChip(text: String) {
    Row {
        Column {
            Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = "connection_error")
        }
        Column {
            Text(
                text = text, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                color = MaterialTheme.colors.onPrimary, fontSize = 16.sp, textAlign = TextAlign.Left
            )
        }
    }
}

@Composable
private fun DeviceCardRow(rowLabel: String, content: String) {
    Row {
        Column(modifier = Modifier.fillMaxWidth(0.3f)) {
            Text(rowLabel, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.fillMaxWidth(1f)) {
            Text(content)
        }
    }
}