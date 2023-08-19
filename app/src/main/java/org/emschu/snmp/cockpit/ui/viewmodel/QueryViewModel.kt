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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.dbHelper
import org.emschu.snmp.cockpit.model.CustomQuery
import org.emschu.snmp.cockpit.ui.sources.CustomQuerySource

/**
 * View Model for custom queries management view
 */
class QueryViewModel : ViewModel() {
    val customQueryList: LiveData<List<CustomQuery>> = CustomQuerySource.allCustomQueries.distinctUntilChanged()

    private val showCustomQueryDialog = mutableStateOf(false)
    val currentCustomQuery = mutableStateOf<CustomQuery?>(null)

    init {
        reload()
    }

    val isDialogShown: MutableState<Boolean>
        get() = showCustomQueryDialog

    fun reload() {
        viewModelScope.launch {
            CustomQuerySource.refresh()
        }
    }

    fun showCreateNewQueryDialog() {
        showCustomQueryDialog.value = true
        currentCustomQuery.value = CustomQuery(0L, "", "", false, true)
    }

    fun showUpdateQueryDialog(customQuery: CustomQuery) {
        showCustomQueryDialog.value = true
        currentCustomQuery.value = customQuery
    }

    fun hideCustomQueryDialog() {
        showCustomQueryDialog.value = false
        currentCustomQuery.value = null
    }

    fun storeQuery(customQueryRecord: CustomQuery) {
        if (customQueryRecord.id == 0L) {
            // create
            dbHelper().addNewQuery(customQueryRecord)
        } else {
            // update
            dbHelper().updateQuery(customQueryRecord)
        }
        reload()
    }

    fun removeCustomQuery(customQuery: CustomQuery) {
        dbHelper().removeQuery(customQuery.id)
        reload()
    }
}