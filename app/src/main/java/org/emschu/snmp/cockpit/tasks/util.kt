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

package org.emschu.snmp.cockpit.tasks

import android.util.Log
import androidx.work.Data
import org.emschu.snmp.cockpit.model.TabList
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.util.deserializeParcelable
import java.io.IOException

// task specific utility functions

fun deviceConfigFromInput(inputData: Data): DeviceConfiguration? = try {
    val byteArray = inputData.getByteArray(DeviceAddTask.INPUT_DEVICE_CONFIGURATION)
    byteArray?.deserializeParcelable()
} catch (ex: ClassCastException) {
    Log.e("Tasks", "Problem casting input byte array to DeviceConfiguration")
    null
} catch (ex: IOException) {
    ex.printStackTrace()
    Log.e("Tasks", "IOException byte array to DeviceConfiguration")
    null
}

fun tabsFromInput(inputData: Data): TabList? = try {
    val byteArray = inputData.getByteArray(DeviceUpdateTask.INPUT_TABS)
    byteArray?.deserializeParcelable()
} catch (ex: ClassCastException) {
    Log.e("Tasks", "Problem casting input byte array to RegisteredQueryList")
    null
} catch (ex: IOException) {
    ex.printStackTrace()
    Log.e("Tasks", "IOException byte array to RegisteredQueryList")
    null
}
