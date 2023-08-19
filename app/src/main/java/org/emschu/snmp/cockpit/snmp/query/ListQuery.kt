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

package org.emschu.snmp.cockpit.snmp.query

import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.snmp4j.smi.OID

open class ListQuery(
    override val oidQuery: OID,
) : SnmpQuery(oidQuery, false) {

    val listItems: MutableList<QueryResponse> = mutableListOf()

    override fun processResult(queryResponses: Collection<QueryResponse>) {
        this.listItems.clear()
        this.listItems.addAll(queryResponses)
        responses.clear()
        responses.addAll(queryResponses)
    }
}