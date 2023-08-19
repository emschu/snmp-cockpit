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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.model.CustomQuery
import org.emschu.snmp.cockpit.ui.components.CustomQueryDialog
import org.emschu.snmp.cockpit.ui.components.verticalScrollbar
import org.emschu.snmp.cockpit.ui.theme.accentBackgroundColor
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CustomQueryView(
    mainViewModel: MainViewModel,
    queryViewModel: QueryViewModel,
    modalBottomSheetState: ModalBottomSheetState,
) {
    Row {
        val listState = rememberLazyListState()
        val items = queryViewModel.customQueryList.observeAsState()
        val scope = rememberCoroutineScope()

        LazyColumn(state = listState, modifier = Modifier.verticalScrollbar(listState)) {
            // TODO: show empty list message
            items((items.value ?: emptyList())) { customQuery ->
                SingleQueryCard(mainViewModel, scope, customQuery, modalBottomSheetState, queryViewModel)
            }
        }
    }
    Row {
        CustomQueryDialog(queryViewModel = queryViewModel)
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun SingleQueryCard(
    mainViewModel: MainViewModel, scope: CoroutineScope,
    customQuery: CustomQuery,
    modalBottomSheetState: ModalBottomSheetState,
    queryViewModel: QueryViewModel,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .defaultMinSize(minHeight = 5.dp),
        backgroundColor = accentBackgroundColor(),
        shape = RoundedCornerShape(8.dp),
        onClick = {}
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Column {
                IconButton(onClick = {
                    runBlocking {
                        mainViewModel.triggerBottomSheet(scope, customQuery.oid, modalBottomSheetState)
                    }
                }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "execute snmp custom query",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
            Column {
                IconButton(onClick = {
                    queryViewModel.showUpdateQueryDialog(customQuery)
                }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "edit snmp custom query",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
            Column {
                Row {
                    Text(
                        customQuery.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h6.fontSize,
                    )
                }
                Row {
                    Text(
                        customQuery.oid,
                        fontSize = MaterialTheme.typography.body1.fontSize,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                    )
                    if (!customQuery.isSingleQuery) {
                        Text(
                            stringResource(R.string.recursive_query_annotation),
                            Modifier.padding(start = 8.dp, end = 8.dp),
                            color = MaterialTheme.colors.secondary,
                        )
                    }
                }
            }
        }
    }
}