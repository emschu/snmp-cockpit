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

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.emschu.snmp.cockpit.snmp.query.SnmpQuery
import org.emschu.snmp.cockpit.ui.screens.DeviceTabItem

@Parcelize
data class TabList(
    val tabList: List<DeviceTabItem>,
) : Parcelable

interface RegisteredQuery {
    val query: SnmpQuery
    val stringResource: Int?
    val title: String?
    val displayType: QueryDisplayType
    val listUnknown: Boolean
}

enum class QueryDisplayType {
    TABLE,
    LIST,
}

data class QueryRegisterTableItem(
    override val query: SnmpQuery,
    @StringRes override val stringResource: Int,
    override val title: String? = null,
    override val displayType: QueryDisplayType = QueryDisplayType.TABLE,
    override val listUnknown: Boolean = true,
) : RegisteredQuery

data class QueryRegisterListItem(
    override val query: SnmpQuery,
    @StringRes override val stringResource: Int,
    override val title: String? = null,
    override val displayType: QueryDisplayType = QueryDisplayType.LIST,
    override val listUnknown: Boolean = true,
) : RegisteredQuery

data class QueryRegisterTitledListItem(
    override val query: SnmpQuery,
    override val title: String,
    override val displayType: QueryDisplayType = QueryDisplayType.LIST,
    override val listUnknown: Boolean = true,
    @StringRes override val stringResource: Int? = null,
) : RegisteredQuery