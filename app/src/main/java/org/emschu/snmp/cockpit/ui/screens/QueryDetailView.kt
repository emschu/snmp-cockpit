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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import org.emschu.snmp.cockpit.ui.components.LazyQueryResultList
import org.emschu.snmp.cockpit.ui.viewmodel.QueryDetailViewModel

@Composable
fun QueryDetailView(
    queryDetailViewModel: QueryDetailViewModel,
) {
    // TODO show alert or instead of leave
    LaunchedEffect(key1 = "") {
        queryDetailViewModel.refreshView()
    }
    val responseList = queryDetailViewModel.responseContent.observeAsState()
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
    ) {
        Row {
            // the oid is
            LazyQueryResultList(
                responseList = responseList.value ?: emptyList(),
                listState = listState,
                showOid = false
            )
        }
    }
}