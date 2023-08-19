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

abstract class SnmpQuery(
    open val oidQuery: OID,
    open val isSingleRequest: Boolean,
    open val isCacheable: Boolean = true,
    open val contentTitleResourceId: Int = 0,
) {
    abstract fun processResult(queryResponses: Collection<QueryResponse>)

    val cacheId: String
        get() = this.javaClass.simpleName + oidQuery.toDottedString()

    val responses: MutableList<QueryResponse> = mutableListOf()

    /**
     * internal helper method to retrieve an oid out of the result list or return null
     *
     * @param results
     * @param oid
     * @return
     */
    fun getOIDValue(results: List<QueryResponse>, oid: OID): String {
        for (queryResponse in results) {
            if (queryResponse.variableBinding.oid == oid) {
                return queryResponse.variableBinding.variable.toString()
            }
        }
        return ""
    }
}