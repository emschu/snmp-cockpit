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

package org.emschu.snmp.cockpit.model

import androidx.compose.runtime.MutableState
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery

/**
 * this class is used across the application to represent a single connection to a snmp device
 */
data class DeviceConnection(
    var deviceConfiguration: DeviceConfiguration,
    val systemQuery: MutableState<SystemQuery>,
) {
    val id: String
        get() = deviceConfiguration.uniqueDeviceId
}