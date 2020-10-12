/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2018-2019
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

package org.emschu.snmp.cockpit.query;


import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;

/**
 * simple cache implementation with invalidation timeout
 */
public class QueryCache {

    public static final int CACHE_TIMEOUT_MS = 180 * 1000;
    public static final String TAG = QueryCache.class.getName();

    private final ConcurrentHashMap<String, SnmpQuery> queryConcurrentHashMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> accessHashMap =
            new ConcurrentHashMap<>();

    public boolean has(String key) {
        return queryConcurrentHashMap.containsKey(key);
    }

    public SnmpQuery get(String key) {
        if (queryConcurrentHashMap.containsKey(key)) {
            Long lastAccess = accessHashMap.get(key);
            if (lastAccess == null
                    || System.currentTimeMillis() - lastAccess < CACHE_TIMEOUT_MS) {
                return queryConcurrentHashMap.get(key);
            }
            Log.d(TAG, "invalidate cache key: " + key);
            queryConcurrentHashMap.remove(key);
            accessHashMap.remove(key);
        }
        return null;
    }

    public void put(String key, SnmpQuery query) {
        queryConcurrentHashMap.remove(key);
        accessHashMap.put(key, System.currentTimeMillis());
        queryConcurrentHashMap.put(key, query);
    }

    public void evictDeviceEntries(String deviceId) {
        for (String singleKeyInCache : queryConcurrentHashMap.keySet()) {
            if (singleKeyInCache.endsWith(deviceId)) {
                Log.d(TAG, "clear device query :" + singleKeyInCache);
                queryConcurrentHashMap.remove(singleKeyInCache);
                accessHashMap.remove(singleKeyInCache);
            }
        }
    }

    public void evictSystemQueries() {
        for (String singleKeyInCache : queryConcurrentHashMap.keySet()) {
            if (singleKeyInCache.contains(SystemQuery.class.getSimpleName())) {
                Log.d(TAG, "clear system query :" + singleKeyInCache);
                queryConcurrentHashMap.remove(singleKeyInCache);
                accessHashMap.remove(singleKeyInCache);
            }
        }
    }
}
