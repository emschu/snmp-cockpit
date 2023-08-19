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

package org.emschu.snmp.cockpit.ui.sources

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.model.CustomQuery

object CustomQuerySource {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            refresh()
        }
    }

    @JvmStatic
    val tabCustomQueries: MutableLiveData<List<CustomQuery>> = MutableLiveData(emptyList())

    @JvmStatic
    val allCustomQueries: MutableLiveData<List<CustomQuery>> = MutableLiveData(emptyList())

    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            tabCustomQueries.postValue(SnmpCockpitApp.dbHelper().getCustomQueriesForTab())
            allCustomQueries.postValue(SnmpCockpitApp.dbHelper().getCustomQueries())
        }
    }
}