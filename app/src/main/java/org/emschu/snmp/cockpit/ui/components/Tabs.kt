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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import org.emschu.snmp.cockpit.ui.screens.DeviceTabItem
import org.emschu.snmp.cockpit.ui.viewmodel.DeviceDetailViewModel


@ExperimentalPagerApi
@Composable
fun TabPage(
    pagerState: PagerState,
    deviceTabItems: List<DeviceTabItem>,
    deviceDetailViewModel: DeviceDetailViewModel,
) {
    Row {
        HorizontalPager(
            count = deviceTabItems.size,
            state = pagerState,
            modifier = Modifier.padding(2.dp)
        ) { index ->
            deviceTabItems[index].screenToLoad(deviceDetailViewModel)
        }
    }
}

@ExperimentalPagerApi
@Composable
fun Tabs(
    tabs: List<DeviceTabItem>, selectedIndex: Int, onPageSelected: ((deviceTabItem: DeviceTabItem) -> Unit),
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex, edgePadding = 0.dp, modifier = Modifier.padding(0.dp)
    ) {
        tabs.forEachIndexed { index, tabItem ->
            Tab(selected = index == selectedIndex, onClick = {
                onPageSelected(tabItem)
            }, text = {
                Text(text = stringResource(id = tabItem.title))
            })
        }
    }
}