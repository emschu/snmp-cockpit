package org.emschu.snmp.cockpit.tasks;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.query.QueryCache;
import org.emschu.snmp.cockpit.query.QueryRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpConnection;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

import java.util.List;
import java.util.concurrent.Executor;

public class QueryTaskExecutor {
    private final static String TAG = QueryTaskExecutor.class.getSimpleName();

    private static final Executor executor = SnmpManager.getInstance().getThreadPoolExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback<R> {
        void onComplete(R result);
    }

    public static <T extends SnmpQuery> void executeAsync(@NonNull QueryRequest<T> queryRequest, @Nullable Callback<T> uiCompletionCallback) {
        executor.execute(() -> {
            //final long startTime = System.currentTimeMillis();
            DeviceConfiguration deviceConfiguration = queryRequest.getDeviceConfiguration();
            if (deviceConfiguration == null) {
                throw new IllegalStateException("null device config!");
            }

            if (queryRequest.isCacheable()) {
                Log.d(TAG, "query has cache id: " + queryRequest.getCacheId());
                QueryCache queryCache = CockpitStateManager.getInstance().getQueryCache();
                T cachedQuery = (T) queryCache.get(queryRequest.getCacheId());
                if (cachedQuery != null) {
                    Log.d(TAG, "return query from cache");
                    // this is executed on the main/ui thread
                    if (uiCompletionCallback != null) {
                        handler.post(() -> {
                            uiCompletionCallback.onComplete(cachedQuery);
                        });
                    }
                    return;
                }
            }

            // get snmp connection instance
            SnmpConnection connector = SnmpManager.getInstance().getOrCreateConnection(deviceConfiguration);
            if (connector == null) {
                Log.d(TAG, "no connection available");
                return;
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
                    return;
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
                return;
            }
            T queryResponseClass;
            try {
                queryResponseClass = (T) queryRequest.getQueryClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                Log.e(TAG, "Error instantiate SnmpQuery class: " + e.getMessage());
                return;
            }
            queryResponseClass.processResult(queryResponses);

            if (queryRequest.isCacheable()) {
                QueryCache queryCache = CockpitStateManager.getInstance().getQueryCache();
                queryCache.put(queryRequest.getCacheId(), queryResponseClass);
            }

            // this is executed on the main/ui thread
            if (uiCompletionCallback != null) {
                handler.post(() -> {
                    uiCompletionCallback.onComplete(queryResponseClass);
                });
            }
        });
    }

}
