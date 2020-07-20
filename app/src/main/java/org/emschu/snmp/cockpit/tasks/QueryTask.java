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

package org.emschu.snmp.cockpit.tasks;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.query.QueryCache;
import org.emschu.snmp.cockpit.query.QueryRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpConnection;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * class to query with a special class in background
 *
 * @param <T>
 */
public class QueryTask<T extends SnmpQuery> extends AsyncTask<QueryRequest<? extends SnmpQuery>, Void, T> {

    public static final String TAG = QueryTask.class.getName();
    private DeviceConfiguration deviceConfiguration = null;
    private long startTime;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        startTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected T doInBackground(QueryRequest<? extends SnmpQuery>... queryRequests) {
        if (queryRequests.length == 0 || queryRequests[0] == null) {
            Log.w(TAG, "invalid query request given");
            return null;
        }

        Log.d(TAG, "query request received");
        QueryRequest queryRequest = queryRequests[0];
        deviceConfiguration = queryRequest.getDeviceConfiguration();
        if (deviceConfiguration == null) {
            throw new IllegalStateException("null device config!");
        }
        // handle caching logic first
        if (queryRequest.isCacheable()) {
            Log.d(TAG, "query has cache id: " + queryRequest.getCacheId());
            QueryCache queryCache = CockpitStateManager.getInstance().getQueryCache();
            T cachedQuery = (T) queryCache.get(queryRequest.getCacheId());
            if (cachedQuery != null) {
                Log.d(TAG, "return query from cache");
                return cachedQuery;
            }
        }

        // get snmp connection instance
        SnmpConnection connector = SnmpManager.getInstance().getOrCreateConnection(deviceConfiguration);
        if (connector == null) {
            Log.d(TAG, "no connection available");
            return null;
        }
        if (!connector.canPing(deviceConfiguration)
                && !CockpitStateManager.getInstance().isInTimeouts()) {
            Log.d(TAG, "reset due to inactivity");
            if (deviceConfiguration.getSnmpVersion() < 3) {
                SnmpManager.getInstance().resetV1Connection(deviceConfiguration);
            } else {
                SnmpManager.getInstance().resetV3Connection(deviceConfiguration);
            }
            connector = SnmpManager.getInstance().getConnection(deviceConfiguration);
            if (connector == null) {
                Log.w(TAG, "no connection available after reset");
                return null;
            }
        }
        connector.startListening();

        List<QueryResponse> queryResponses;
        Log.i(TAG, "querying " + queryRequest.getOidQuery().toDottedString());
        if (queryRequest.isSingleRequest()) {
            queryResponses = connector.querySingle(deviceConfiguration,
                    queryRequest.getOidQuery());
        } else {
            queryResponses = connector.queryWalk(deviceConfiguration,
                    queryRequest.getOidQuery());
        }

        if (queryResponses == null) {
            return null;
        }
        SnmpQuery queryResponseClass = null;
        try {
            queryResponseClass = (SnmpQuery) queryRequest.getQueryClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "Error instantiate SnmpQuery class: " + e.getMessage());
            return null;
        }
        queryResponseClass.processResult(queryResponses);
        if (queryRequest.isCacheable()) {
            QueryCache queryCache = CockpitStateManager.getInstance().getQueryCache();
            queryCache.put(queryRequest.getCacheId(), queryResponseClass);
        }
        return (T) queryResponseClass;
    }

    @Override
    protected void onPostExecute(T t) {
        super.onPostExecute(t);
        Log.d(TAG, "QueryTask: consumed time: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Nullable
    public DeviceConfiguration getDeviceConfiguration() {
        boolean isLogged = false;
        while (deviceConfiguration == null) {
            if (!isLogged) {
                Log.d(TAG, "wait for device config set..");
                isLogged = true;
            }
        }
        return deviceConfiguration;
    }

    /**
     * helper method to get query object or null
     *
     * @return
     */
    public T getQuery() {
        DeviceConfiguration deviceConfiguration = getDeviceConfiguration();
        if (deviceConfiguration == null) {
            Log.w(TAG, "No DeviceConfiguration found!");
            return null;
        }
        try {
            // wait for reference or illegal state exception is thrown
            int offset = deviceConfiguration.getAdditionalTimeoutOffset();
            T query = get((long) CockpitPreferenceManager.TIMEOUT_WAIT_ASYNC_MILLISECONDS + offset, TimeUnit.MILLISECONDS);
            if (query != null) {
                SnmpManager.getInstance().resetTimeout(this.deviceConfiguration);
                return query;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Log.w(TAG, "execution exception: " + e.getMessage());
        } catch (TimeoutException e) {
            Log.w(TAG, "timeout reached for query task");
            SnmpManager.getInstance().registerTimeout(deviceConfiguration);
        }
        return null;
    }
}
