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

package org.emschu.snmp.cockpit.snmp;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.util.TimeoutObservable;
import org.snmp4j.security.AuthHMAC128SHA224;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthHMAC256SHA384;
import org.snmp4j.security.AuthHMAC384SHA512;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.smi.OID;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * singleton snmp manager class implementation
 * <p>
 * TODO add support for other algorithms(?!)
 * PrivAES192With3DESKeyExtension.ID,
 * PrivAES256With3DESKeyExtension.ID,
 */
public class SnmpManager {
    public static final String TAG = SnmpManager.class.getName();
    private final ConcurrentHashMap<String, TimeoutObservable> timeoutCounterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SnmpConnection> snmpConnectionPool = new ConcurrentHashMap<>();

    private ThreadPoolExecutor threadPoolExecutor;

    private static SnmpManager instance = null;

    private OID[] privProtocols = new OID[]{
            PrivAES128.ID,
            PrivDES.ID,
            PrivAES192.ID,
            PrivAES256.ID,
            Priv3DES.ID,
    };
    private OID[] authProtocols = new OID[]{
            AuthSHA.ID,
            AuthMD5.ID,
            AuthHMAC128SHA224.ID,
            AuthHMAC192SHA256.ID,
            AuthHMAC256SHA384.ID,
            AuthHMAC384SHA512.ID
    };
    private CockpitPreferenceManager cockpitPreferenceManager;

    /**
     * private singleton constructor
     */
    private SnmpManager() {
        threadPoolExecutor = createNewThreadPool();
    }

    /**
     * singleton access method
     *
     * @return
     */
    public static synchronized SnmpManager getInstance() {
        Log.v(TAG, "snmp manager instance requested");
        if (instance == null) {
            instance = new SnmpManager();
        }
        return instance;
    }

    @Nullable
    public synchronized SnmpConnection getConnection(String uniqueDeviceId) {
        if (CockpitStateManager.getInstance().getIsInTimeoutsObservable().getValue()) {
            Log.w(TAG, "no connection available. clear timeout state first.");
            return null;
        }
        if (CockpitStateManager.getInstance().getIsInSessionTimeoutObservable().getValue()) {
            Log.w(TAG, "no connection available. session timeout detected.");
            return null;
        }
        // check general app session timeout
        if (cockpitPreferenceManager != null) {
            cockpitPreferenceManager.checkSessionTimeout();
        }
        if (snmpConnectionPool.containsKey(uniqueDeviceId)) {
            SnmpConnection connection = snmpConnectionPool.get(uniqueDeviceId);
            if (connection == null) {
                Log.e(TAG, "null connection found! Connection not retrievable.");
                return null;
            }
            DeviceConfiguration deviceConfiguration = connection.getDeviceConfiguration();
            if (deviceConfiguration == null) {
                Log.w(TAG, "DeviceConfiguration is null!");
                return null;
            }
            if (deviceConfiguration.isV1() || deviceConfiguration.isV2c()) {
                Log.d(TAG, "existing snmp v1Connection is used");
            } else {
                Log.d(TAG, "existing snmp v3Connection is used");
            }
            return connection;
        }
        return null;
    }

    /**
     * get connection of stored connection pool
     * you should always use this method to get a valid snmp connection object
     * exception: during connection test
     *
     * @param deviceConfiguration
     * @return
     */
    public synchronized SnmpConnection getConnection(@NonNull DeviceConfiguration deviceConfiguration) {
        return getConnection(deviceConfiguration.getUniqueDeviceId());
    }

    public synchronized SnmpConnection getOrCreateConnection(@NonNull DeviceConfiguration deviceConfiguration) {
        String uniqueDeviceId = deviceConfiguration.getUniqueDeviceId();
        boolean isV3 = deviceConfiguration.isV3();

        SnmpConnection connection = getConnection(uniqueDeviceId);
        if (connection == null) {
            Log.w(TAG, String.format("requested connection '%s' is not available. Reinit of connection is started.", uniqueDeviceId));
        } else {
            return connection;
        }
        // create new connection:
        if (isV3) {
            resetV3Connection(deviceConfiguration);
            SnmpConnection v3Connection = snmpConnectionPool.get(deviceConfiguration.getUniqueDeviceId());
            if (v3Connection == null) {
                Log.e(TAG, "v3 connection is null!");
                return null;
            }
            return v3Connection;
        }
        resetV1Connection(deviceConfiguration);
        SnmpConnection v1Connection = snmpConnectionPool.get(deviceConfiguration.getUniqueDeviceId());
        if (v1Connection == null) {
            Log.w(TAG, "v1 connection is null!");
            return null;
        }
        return v1Connection;
    }

    /**
     * setup a new v1Connection. very expensive
     *
     * @param deviceConfiguration
     */
    public synchronized void resetV1Connection(DeviceConfiguration deviceConfiguration) {
        Log.d(TAG, "SNMPv1Connection is reset with " + snmpConnectionPool.size() + " connections.");
        // re-init v1Connection
        SnmpConnection connection = snmpConnectionPool.get(deviceConfiguration.getUniqueDeviceId());
        if (connection != null) {
            Log.d(TAG, "re-init v1 connection");
            connection.close();
            snmpConnectionPool.remove(deviceConfiguration.getUniqueDeviceId());
        }

        // if api level > N is possible the following can be replaced with .forEach(lambda)
        for (SnmpConnection connection1 : snmpConnectionPool.values()) {
            DeviceConfiguration dv = connection1.getDeviceConfiguration();
            if (dv.getSnmpVersion() < 3) {
                TimeoutObservable timeoutObservable = timeoutCounterMap.get(dv.getUniqueDeviceId());
                if (timeoutObservable != null) {
                    timeoutObservable.setValueAndTriggerObservers(false);
                }
            }
        }
        SnmpConnection v1Connection = new SnmpConnection(deviceConfiguration);
        snmpConnectionPool.put(deviceConfiguration.getUniqueDeviceId(), v1Connection);
    }

    /**
     * setup a new v3Connection. very expensive
     *
     * @param deviceConfiguration
     */
    public synchronized void resetV3Connection(DeviceConfiguration deviceConfiguration) {
        Log.d(TAG, "SNMPv3Connection is set up / reset with " + snmpConnectionPool.size() + " connections");
        // re-init v3Connection
        SnmpConnection v3Connection = snmpConnectionPool.get(deviceConfiguration.getUniqueDeviceId());
        if (v3Connection != null) {
            Log.v(TAG, "re-init v3 connection");
            v3Connection.close();
            snmpConnectionPool.remove(deviceConfiguration.getUniqueDeviceId());
        }

        // if Android > N replaceable with .forEach(lambda)
        for (SnmpConnection connection : snmpConnectionPool.values()) {
            DeviceConfiguration dv = connection.getDeviceConfiguration();
            if (dv.getSnmpVersion() == 3) {
                if (timeoutCounterMap.containsKey(dv.getUniqueDeviceId())) {
                    timeoutCounterMap.get(dv.getUniqueDeviceId()).setValueAndTriggerObservers(false);
                }
            }
        }
        SnmpConnection connection = new SnmpConnection(deviceConfiguration);
        snmpConnectionPool.put(deviceConfiguration.getUniqueDeviceId(), connection);
    }

    /**
     * app-wide thread pool executor for android async tasks
     *
     * @return
     */
    public synchronized ThreadPoolExecutor getThreadPoolExecutor() {
        if (threadPoolExecutor == null || threadPoolExecutor.isShutdown()
                || threadPoolExecutor.isTerminated() || threadPoolExecutor.isTerminating()) {
            threadPoolExecutor = createNewThreadPool();
        }
        return threadPoolExecutor;
    }

    /**
     * this method instantiates a new thread pool we use for async tasks
     *
     * @return
     */
    @NonNull
    private ThreadPoolExecutor createNewThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() - 1;
        Log.d(TAG, String.format("init new ThreadPoolFactory with core pool size %s", corePoolSize));
        return new ThreadPoolExecutor(Math.max(corePoolSize, 1), Integer.MAX_VALUE,
                600L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    /**
     * the total number of tested connections
     *
     * @return
     */
    public int getTotalConnectionTestCount() {
        return authProtocols.length * privProtocols.length;
    }

    /**
     * test all transport security options
     * atm ~ 25-30
     *
     * @param deviceConfiguration
     */
    public synchronized List<Pair<OID, OID>> testConnections(DeviceConfiguration deviceConfiguration, ProgressConnectionTestCallback progressCallback,
                                                             int connectionTestTimeout, int connectionTestRetries) {
        List<Pair<OID, OID>> combinationList = new ArrayList<>();
        Log.d(TAG, "using v1Connection test timeout: " + connectionTestTimeout
                + " ms with " + connectionTestRetries + " retries");

        if (!CockpitStateManager.getInstance().getNetworkSecurityObservable().getValue()) {
            Log.w(TAG, "connection test not allowed to run! network is not secure!");
            return combinationList;
        }

        // reset counter here
        int connectionTestCounter = 0;
        boolean shallBreak = false;
        for (int i = 0; i < authProtocols.length; i++) {
            for (int k = 0; k < privProtocols.length; k++) {
                // ... this is expensive^2:
                connectionTestCounter++;
                progressCallback.run(connectionTestCounter);
                // clone device config object
                DeviceConfiguration testConfig = new DeviceConfiguration(deviceConfiguration);

                testConfig.setAuthProtocol(authProtocols[i]);
                testConfig.setPrivProtocol(privProtocols[k]);
                testConfig.setTimeout(connectionTestTimeout);
                testConfig.setRetries(connectionTestRetries);

                SnmpConnection connection = new SnmpConnection(testConfig);
                try {
                    boolean isWorking = connection.canPing(testConfig);
                    if (isWorking) {
                        Log.d(TAG, "successful test with combination: "
                                + testConfig.getAuthProtocol() + "/" + testConfig.getPrivProtocol()
                                + testConfig.getAuthProtocolLabel() + "/"
                                + testConfig.getPrivProtocolLabel());

                        combinationList.add(new Pair<>(testConfig.getAuthProtocol(), testConfig.getPrivProtocol()));
                        // in case of successful connection, we do not close this connection
                        shallBreak = true;
                        break;
                    }
                } finally {
                    connection.close();
                }

                Log.d(TAG, "no connection possible with combination: "
                        + testConfig.getAuthProtocol() + "/" + testConfig.getPrivProtocol());
            }
            if (shallBreak) {
                break;
            }
        }
        Log.d(TAG, "found " + combinationList.size() + " combinations found: " + combinationList);
        return combinationList;
    }

    /**
     * check if we know this v1Connection + if transport is listening
     *
     * @param deviceId
     * @return
     */
    public boolean doesConnectionExist(String deviceId) {
        if (!snmpConnectionPool.containsKey(deviceId)) {
            return false;
        }
        SnmpConnection connection = snmpConnectionPool.get(deviceId);
        if (connection != null && connection.getTransport().isListening()) {
            if (DeviceManager.getInstance().hasDevice(deviceId)) {
                return true;
            }
        }
        snmpConnectionPool.remove(deviceId);
        return false;
    }

    public OID[] getPrivProtocols() {
        return privProtocols;
    }

    public void setPrivProtocols(OID[] privProtocols) {
        this.privProtocols = privProtocols;
    }

    public OID[] getAuthProtocols() {
        return authProtocols;
    }

    public void setAuthProtocols(OID[] authProtocols) {
        this.authProtocols = authProtocols;
    }

    /**
     * method to clear all snmp connections
     */
    public synchronized void clearConnections() {
        Log.d(TAG, "clear all snmp connections");
        for (SnmpConnection snmpConnection : snmpConnectionPool.values()) {
            if (snmpConnection != null) {
                snmpConnection.close();
            }
        }
        timeoutCounterMap.clear();
    }

    /**
     * remove a v1Connection of the v1Connection pool
     * NOTE: never call this method on main thread!
     *
     * @param deviceConfiguration
     */
    public synchronized void removeConnection(DeviceConfiguration deviceConfiguration) {
        Log.d(TAG, "removing connection from pool: " + deviceConfiguration.getUniqueDeviceId());
        timeoutCounterMap.remove(deviceConfiguration.getUniqueDeviceId());
        Log.d(TAG, "after deletion: " + timeoutCounterMap.toString());

        if (deviceConfiguration.isV3()) {
            resetV3Connection(deviceConfiguration);
        } else {
            resetV1Connection(deviceConfiguration);
        }
    }

    /**
     * method to register a timeout event
     *
     * @param deviceConfiguration
     */
    public synchronized void registerTimeout(DeviceConfiguration deviceConfiguration) {
        if (deviceConfiguration == null) {
            throw new IllegalArgumentException("null device config given");
        }
        if (CockpitStateManager.getInstance().isInRemoval()
                || CockpitStateManager.getInstance().isConnecting()) {
            Log.d(TAG, "timeout event not registered during device connection or removal event");
            return;
        }
        if (!timeoutCounterMap.containsKey(deviceConfiguration.getUniqueDeviceId())) {
            timeoutCounterMap.put(deviceConfiguration.getUniqueDeviceId(),
                    new TimeoutObservable(true, deviceConfiguration));
        }
        timeoutCounterMap.get(deviceConfiguration.getUniqueDeviceId())
                .setValueAndTriggerObservers(true);

        CockpitStateManager.getInstance().getIsInTimeoutsObservable().setValueAndTriggerObservers(true);

        SnmpManager.getInstance().getThreadPoolExecutor().shutdownNow();

        Log.d(TAG, "timeout detected of device: " + deviceConfiguration.getUniqueDeviceId());
    }

    /**
     * fire this event if a connection worked
     *
     * @param deviceConfiguration
     */
    public synchronized void resetTimeout(DeviceConfiguration deviceConfiguration) {
        if (deviceConfiguration == null) {
            throw new IllegalArgumentException("null device config given");
        }
        if (!timeoutCounterMap.containsKey(deviceConfiguration.getUniqueDeviceId())) {
            timeoutCounterMap.put(deviceConfiguration.getUniqueDeviceId(), new TimeoutObservable(false, deviceConfiguration));
        }
        Objects.requireNonNull(timeoutCounterMap.get(deviceConfiguration.getUniqueDeviceId()))
                .setValueAndTriggerObservers(false);

        CockpitStateManager.getInstance().getIsInTimeoutsObservable().setValueAndTriggerObservers(false);
        Log.d(TAG, "general timeout state value: " + CockpitStateManager.getInstance().getIsInTimeoutsObservable().getValue());

        Log.d(TAG, "timeout counter reset for device: " + deviceConfiguration.getUniqueDeviceId());
    }

    /**
     * retrieves current devices which are in timeout state
     *
     * @return
     */
    public ManagedDevice[] getDevicesInTimeout() {
        ArrayList<ManagedDevice> devices = new ArrayList<>();
        Enumeration<String> keys = timeoutCounterMap.keys();
        for (TimeoutObservable timeoutObservable : timeoutCounterMap.values()) {
            String deviceIdKey = keys.nextElement();
            if (timeoutObservable.getValue()) {
                if (!snmpConnectionPool.containsKey(deviceIdKey)
                        || !DeviceManager.getInstance().hasDevice(deviceIdKey)) {
                    continue;
                }
                devices.add(DeviceManager.getInstance().getDevice(deviceIdKey));
            }
        }
        return devices.toArray(new ManagedDevice[]{});
    }

    /**
     * should be injected one time
     *
     * @param cockpitPreferenceManager
     */
    public void setPreferenceManager(CockpitPreferenceManager cockpitPreferenceManager) {
        this.cockpitPreferenceManager = cockpitPreferenceManager;
    }

    /**
     * face method to call request counter
     */
    public void incrementRequestCounter() {
        if (cockpitPreferenceManager != null) {
            cockpitPreferenceManager.incrementRequestCounter();
        } else {
            Log.w(TAG, "no preference manager instance existing!");
        }
    }

    private boolean hasV3Connection() {
        for (SnmpConnection connection : snmpConnectionPool.values()) {
            if (connection.getDeviceConfiguration().isV3()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasV1OrV2cConnection() {
        for (SnmpConnection connection : snmpConnectionPool.values()) {
            if (connection.getDeviceConfiguration().isV1() ||
                    connection.getDeviceConfiguration().isV2c()) {
                return true;
            }
        }
        return false;
    }

    public interface ProgressConnectionTestCallback {
        void run(int connectionTestCount);
    }
}