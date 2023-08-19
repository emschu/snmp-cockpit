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

import android.util.Log
import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.snmp4j.smi.OID


abstract class TableQuery(
    val oid: OID,
) : SnmpQuery(oid, false) {
    val content: MutableMap<String, MutableMap<String, QueryResponse>> by lazy { mutableMapOf() }
    private var unknownOIDCounter = -1

    /**
     * helper method to build row titles - if data is present
     *
     * @param row
     * @param columns
     * @param divider
     * @return
     */
    protected fun concatenateIfPossible(
        row: Map<String, QueryResponse>, columns: Array<String?>, divider: String?,
    ): String {
        val sb = StringBuilder()
        for (i in columns.indices) {
            val fieldOrEmpty = getFieldOrEmpty(row, columns[i])
            if (fieldOrEmpty.isNotBlank()) {
                if (i != 0) {
                    sb.append(" ")
                        .append(divider)
                        .append(" ")
                }
                sb.append(fieldOrEmpty)
            }
        }
        return sb.toString()
    }

    /**
     * helper method to build titles
     *
     * @param row
     * @param fieldName
     * @return
     */
    protected fun getFieldOrEmpty(row: Map<String, QueryResponse>, fieldName: String?): String {
        if (!row.containsKey(fieldName)) {
            return ""
        }
        val qr = row[fieldName]
        return qr?.value ?: ""
    }

    abstract fun getRowTitle(singleRow: MutableMap<String, QueryResponse>, i: Int): String

    companion object {
        private const val TAG = "TableQuery"
    }

    override fun processResult(queryResponses: Collection<QueryResponse>) {
        responses.clear()
        responses.addAll(queryResponses)

        content.clear()
        for (qr in queryResponses) {
            // strip last oid node - its the counter number
            val rowIndex = qr.oid.substring(qr.oid.lastIndexOf('.') + 1)
            var currentRow: MutableMap<String, QueryResponse>?
            if (!content.containsKey(rowIndex)) {
                // lazy init rows
                currentRow = HashMap()
                content[rowIndex] = currentRow
            } else {
                currentRow = content[rowIndex]
            }
            if (currentRow == null) {
                continue
            }
            val key: String = qr.asnName ?: qr.oid
            if (key != "") {
                currentRow[key] = qr
            }
        }
        Log.d(TAG, "loaded ${content.size} rows into table with input size ${queryResponses.size}")
    }
}