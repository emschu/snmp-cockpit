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
import org.emschu.snmp.cockpit.snmp.query.impl.general.SystemQuery
import java.util.concurrent.ConcurrentHashMap

/**
 * simple cache implementation with invalidation timeout
 */
class QueryCache {
    private val queryConcurrentHashMap = ConcurrentHashMap<String, SnmpQuery>()
    private val accessHashMap = ConcurrentHashMap<String, Long>()
    fun has(key: String): Boolean {
        return queryConcurrentHashMap.containsKey(key)
    }

    operator fun get(key: String): SnmpQuery? {
        if (queryConcurrentHashMap.containsKey(key)) {
            val lastAccess = accessHashMap[key]
            if (lastAccess == null || System.currentTimeMillis() - lastAccess < CACHE_TIMEOUT_MS) {
                return queryConcurrentHashMap[key]
            }
            Log.d(TAG, "invalidate cache key: $key")
            queryConcurrentHashMap.remove(key)
            accessHashMap.remove(key)
        }
        return null
    }

    fun put(key: String, query: SnmpQuery) {
        queryConcurrentHashMap.remove(key)
        accessHashMap[key] = System.currentTimeMillis()
        queryConcurrentHashMap[key] = query
    }

    fun evictDeviceEntries(deviceId: String?) {
        for (singleKeyInCache in queryConcurrentHashMap.keys) {
            if (singleKeyInCache.endsWith(deviceId!!)) {
                Log.d(TAG, "clear device query :$singleKeyInCache")
                queryConcurrentHashMap.remove(singleKeyInCache)
                accessHashMap.remove(singleKeyInCache)
            }
        }
    }

    fun evictSystemQueries() {
        for (singleKeyInCache in queryConcurrentHashMap.keys) {
            if (singleKeyInCache.contains(SystemQuery::class.java.simpleName)) {
                Log.d(TAG, "clear system query :$singleKeyInCache")
                queryConcurrentHashMap.remove(singleKeyInCache)
                accessHashMap.remove(singleKeyInCache)
            }
        }
    }

    companion object {
        const val CACHE_TIMEOUT_MS = 180 * 1000
        private val TAG = QueryCache::class.java.name
    }
}