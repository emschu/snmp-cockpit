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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.model.QueryRegisterTableItem
import org.emschu.snmp.cockpit.model.RegisteredQuery
import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.emschu.snmp.cockpit.snmp.query.TableQuery
import org.emschu.snmp.cockpit.ui.theme.accentBackgroundColor
import org.emschu.snmp.cockpit.ui.viewmodel.QueryResponseCollection

@Composable
fun LazyQueryResultList(
    responseList: QueryResponseCollection,
    listState: LazyListState,
    showOid: Boolean = true,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.verticalScrollbar(listState),
    ) {
        if (responseList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TODO make nice loading view
                        Text(
                            text = stringResource(R.string.loading),
                            modifier = Modifier.padding(12.dp),
                            fontSize = MaterialTheme.typography.h6.fontSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(responseList, key = { it.first.hashCode() }) { singleResponse ->
                QueryCard(singleResponse.first, singleResponse.second, showOid)
            }
        }
    }
}

@Composable
private fun QueryCard(
    query: RegisteredQuery,
    responseList: List<QueryResponse>,
    showOid: Boolean = true,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .defaultMinSize(minHeight = 5.dp),
        backgroundColor = accentBackgroundColor(),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth(if (responseList.isEmpty()) 0.8f else 1f)) {
                    // card title row
                    Text(
                        text = if (query.stringResource != null) {
                            stringResource(query.stringResource!!)
                        } else {
                            query.title ?: stringResource(R.string.label_no_title)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h6.fontSize,
                    )
                }
                if (responseList.isEmpty()) {
                    Column {
                        Text(
                            stringResource(R.string.label_empty),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colors.primaryVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            if (showOid) {
                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        query.query.oidQuery.toDottedString(),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (query is QueryRegisterTableItem && query.query is TableQuery) {
                // table mode
                val tableQuery = query.query as TableQuery

                val isExpanded = tableQuery.content.values.size < 7
                for ((i, singleRow) in tableQuery.content.values.withIndex()) {
                    val rowTitle = tableQuery.getRowTitle(singleRow, i)
                    ExpandableTableEntry(rowTitle, responseList, i + 1, isExpanded)
                }
            } else {
                // list mode
                responseList.forEach { response -> QueryResponseRow(response) }
            }
        }
    }
}

/**
 * @param rowId starts at 1
 */
@Composable
fun ExpandableTableEntry(title: String, tableEntryValues: List<QueryResponse>, rowId: Int, isExpanded: Boolean) {
    // only relevant for short mode
    val showMore = remember { mutableStateOf(isExpanded) }

    Column(
        modifier = Modifier
            .padding(0.dp)
            .background(accentBackgroundColor())
            .clickable {
                showMore.value = !showMore.value
            },
        horizontalAlignment = Alignment.Start
    ) {
        if (tableEntryValues.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    if (showMore.value) {
                        Icons.Filled.ExpandLess
                    } else {
                        Icons.Filled.ExpandMore
                    },
                    "table item expand icon",
                )
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (showMore.value) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                for (entry in tableEntryValues) {
                    if (entry.oid.endsWith(".$rowId")) {
                        QueryResponseRow(response = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryResponseRow(response: QueryResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            Text(
                (if (!response.asnName.isNullOrBlank()) {
                    "${response.asnName}: "
                } else {
                    ""
                }) + response.value,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
        Column {
            Text(
                response.oid,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Right,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            )
        }
    }
}