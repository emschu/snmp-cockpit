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

/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2021-22
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit.tasks


import android.util.Log
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.cockpitStateManager
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.snmpManager
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.OIDCatalog
import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.emschu.snmp.cockpit.snmp.query.SnmpQuery

object QueryTaskExecutor {
    private val TAG = QueryTaskExecutor::class.java.simpleName

    fun querySnmpDevice(
        deviceConfiguration: DeviceConfiguration,
        queryRequest: SnmpQuery,
        isCacheAllowed: Boolean = true,
    ): SnmpQuery? {
        if (isCacheAllowed && queryRequest.isCacheable) {
            Log.d(TAG, "query has cache id: " + queryRequest.cacheId)
            val queryCache = cockpitStateManager.queryCache
            val cachedQuery = queryCache[queryRequest.cacheId + deviceConfiguration.uniqueDeviceId]
            if (cachedQuery != null) {
                Log.d(TAG, "return query from cache")
                return cachedQuery
            }
        }

        // get snmp connection instance
        var connector = snmpManager.getOrCreateConnection(deviceConfiguration)
        if (connector == null) {
            Log.d(TAG, "no connection available")
            return null
        }
        if (!connector.canPing(deviceConfiguration)) {
            if (deviceConfiguration.isV1 || deviceConfiguration.isV2c) {
                snmpManager.resetV1Connection(deviceConfiguration)
            } else {
                snmpManager.resetV3Connection(deviceConfiguration)
            }
            connector = snmpManager.loadConnection(deviceConfiguration)
            if (connector == null) {
                Log.w(TAG, "no connection available after reset")
                return null
            }
        }
        connector.startListening()

        Log.i(TAG, "querying " + queryRequest.oidQuery.toDottedString())
        val queryResponses: List<QueryResponse> = if (queryRequest.isSingleRequest) {
            connector.querySingle(deviceConfiguration, queryRequest.oidQuery)
        } else {
            connector.queryWalk(deviceConfiguration, queryRequest.oidQuery)
        }

        val responseListWithAsn = queryResponses.map { queryResponse ->
            val asnName: String? = OIDCatalog.getAsnByOid(queryResponse.oid)
            if (!asnName.isNullOrBlank()) {
                queryResponse.copy(asnName = asnName)
            } else {
                queryResponse
            }
        }
        queryRequest.processResult(responseListWithAsn)

        if (queryRequest.isCacheable && queryResponses.isNotEmpty()) {
            val queryCache = cockpitStateManager.queryCache
            queryCache.put(queryRequest.cacheId + deviceConfiguration.uniqueDeviceId, queryRequest)
        }

        return queryRequest
    }
}