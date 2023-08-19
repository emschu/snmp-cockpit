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
package org.emschu.snmp.cockpit.snmp

import android.util.Log
import android.util.Pair
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.snmp4j.security.AuthHMAC128SHA224
import org.snmp4j.security.AuthHMAC192SHA256
import org.snmp4j.security.AuthHMAC256SHA384
import org.snmp4j.security.AuthHMAC384SHA512
import org.snmp4j.security.AuthMD5
import org.snmp4j.security.AuthSHA
import org.snmp4j.security.Priv3DES
import org.snmp4j.security.PrivAES128
import org.snmp4j.security.PrivAES192
import org.snmp4j.security.PrivAES256
import org.snmp4j.security.PrivDES
import org.snmp4j.smi.OID
import java.util.concurrent.ConcurrentHashMap

/**
 * singleton snmp manager class implementation
 *
 * TODO add support for other algorithms(?!)
 * PrivAES192With3DESKeyExtension.ID,
 * PrivAES256With3DESKeyExtension.ID,
 */
object SnmpManager {
    private val snmpConnectionPool = ConcurrentHashMap<String, SnmpConnection>()
    val privProtocols: Array<OID> = arrayOf(
        PrivAES128.ID, PrivDES.ID, PrivAES192.ID, PrivAES256.ID, Priv3DES.ID
    )
    val authProtocols: Array<OID> = arrayOf(
        AuthSHA.ID, AuthMD5.ID, AuthHMAC128SHA224.ID, AuthHMAC192SHA256.ID, AuthHMAC256SHA384.ID, AuthHMAC384SHA512.ID
    )
    val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    /**
     * the total number of tested connections
     *
     * @return
     */
    val totalConnectionTestCount: Int = authProtocols.size * privProtocols.size

    @Synchronized
    fun getConnection(uniqueDeviceId: String): SnmpConnection? {
        if (snmpConnectionPool.containsKey(uniqueDeviceId)) {
            val connection = snmpConnectionPool[uniqueDeviceId]
            if (connection == null) {
                Log.e(TAG, "null connection found! Connection not retrievable.")
                return null
            }
            val deviceConfiguration = connection.deviceConfiguration
            if (deviceConfiguration.isV1 || deviceConfiguration.isV2c) {
                Log.d(TAG, "snmp v1Connection is used")
            } else {
                Log.d(TAG, "snmp v3Connection is used")
            }
            return connection
        }
        return null
    }

    /**
     * get connection of stored connection pool
     * you should always use this method to get a valid snmp connection object
     * exception: during connection test
     *
     * @param deviceConfiguration
     * @return
     */
    fun loadConnection(deviceConfiguration: DeviceConfiguration): SnmpConnection? {
        return getConnection(deviceConfiguration.uniqueDeviceId)
    }

    /**
     * will return pooled connection instance
     */
    fun getOrCreateConnection(deviceConfiguration: DeviceConfiguration): SnmpConnection? {
        val uniqueDeviceId = deviceConfiguration.uniqueDeviceId
        val connection = getConnection(uniqueDeviceId)
        if (connection != null) {
            return connection
        }
        if (deviceConfiguration.isV3) {
            resetV3Connection(deviceConfiguration)
            val v3Connection = snmpConnectionPool[deviceConfiguration.uniqueDeviceId]
            if (v3Connection == null) {
                Log.e(TAG, "v3 connection is null!")
                return null
            }
            return v3Connection
        }
        // handle v1 + v2c
        resetV1Connection(deviceConfiguration)
        val v1Connection = snmpConnectionPool[deviceConfiguration.uniqueDeviceId]
        if (v1Connection == null) {
            Log.w(TAG, "v1 connection is null!")
            return null
        }
        return v1Connection
    }

    /**
     * setup a new v1Connection. very expensive
     *
     * @param deviceConfiguration
     */
    @Synchronized
    fun resetV1Connection(deviceConfiguration: DeviceConfiguration, reInit: Boolean = true) {
        Log.d(TAG, "SNMPv1Connection is reset with " + snmpConnectionPool.size + " connections.")
        // re-init v1Connection
        val connection = snmpConnectionPool[deviceConfiguration.uniqueDeviceId]
        if (connection != null) {
            Log.d(TAG, "close v1 connection")
            connection.close()
            snmpConnectionPool.remove(deviceConfiguration.uniqueDeviceId)
        }

        if (reInit) {
            snmpConnectionPool[deviceConfiguration.uniqueDeviceId] = SnmpConnection(deviceConfiguration)
        }
    }

    /**
     * setup a new v3Connection. very expensive
     *
     * @param deviceConfiguration
     */
    @Synchronized
    fun resetV3Connection(deviceConfiguration: DeviceConfiguration, reInit: Boolean = true) {
        Log.d(
            TAG, "SNMPv3Connection is set up / reset with " + snmpConnectionPool.size + " connections"
        )
        // re-init v3Connection
        val v3Connection = snmpConnectionPool[deviceConfiguration.uniqueDeviceId]
        if (v3Connection != null) {
            Log.v(TAG, "close v3 connection")
            v3Connection.close()
            snmpConnectionPool.remove(deviceConfiguration.uniqueDeviceId)
        }

        if (reInit) {
            snmpConnectionPool[deviceConfiguration.uniqueDeviceId] = SnmpConnection(deviceConfiguration)
        }
    }

    /**
     * test all transport security options
     * ~ 25-30
     *
     * @param deviceConfiguration
     */
    @Synchronized
    fun testConnections(
        deviceConfiguration: DeviceConfiguration,
        progressCallback: (Int, String) -> ListenableFuture<Void>,
        connectionTestTimeout: Int,
        connectionTestRetries: Int,
    ): List<Pair<OID, OID>> {
        val combinationList: MutableList<Pair<OID, OID>> = mutableListOf()
        Log.d(TAG, "using v1Connection test timeout: $connectionTestTimeout ms with $connectionTestRetries retries")
        if (SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.value == false) {
            Log.w(TAG, "connection test not allowed to run! network is not available or secure!")
            return combinationList
        }

        // reset counter here
        var connectionTestCounter = 0
        var shallBreak = false

        for (i in authProtocols.indices) {
            for (k in privProtocols.indices) {
                connectionTestCounter++
                // clone device config object
                val testConfig = deviceConfiguration.copy(
                    authProtocol = authProtocols[i],
                    privProtocol = privProtocols[k],
                    timeout = connectionTestTimeout,
                    retries = connectionTestRetries,
                )

                progressCallback.invoke(
                    connectionTestCounter,
                    "${testConfig.authProtocolLabel}/${testConfig.privProtocolLabel}"
                )
                val connection = SnmpConnection(testConfig)
                try {
                    val isWorking = connection.canPing(testConfig)
                    if (isWorking) {
                        Log.d(
                            TAG,
                            "successful test with combination: " + testConfig.authProtocol + "/" + testConfig.privProtocol + testConfig.authProtocolLabel + "/" + testConfig.privProtocolLabel
                        )
                        combinationList.add(Pair(testConfig.authProtocol, testConfig.privProtocol))
                        // in case of successful connection, we do not close this connection
                        shallBreak = true
                        break
                    }
                } finally {
                    coroutineScope.launch {
                        connection.close()
                    }
                }
                Log.d(
                    TAG,
                    "no connection possible with combination: " + testConfig.authProtocol + "/" + testConfig.privProtocol
                )
            }
            if (shallBreak) {
                break
            }
        }
        Log.d(TAG, "found " + combinationList.size + " combinations found: " + combinationList)

        return combinationList
    }

    /**
     * check if we know this v1Connection + if transport is listening
     *
     * @param deviceId
     * @return
     */
    fun doesConnectionExist(deviceId: String): Boolean {
        if (!snmpConnectionPool.containsKey(deviceId)) {
            return false
        }
        val connection = snmpConnectionPool[deviceId]
        if (connection != null && connection.getTransport()?.isListening == true) {
            if (SnmpCockpitApp.deviceManager.hasDevice(deviceId)) {
                return true
            }
        }
        snmpConnectionPool.remove(deviceId)
        return false
    }

    /**
     * method to clear all snmp connections
     */
    @Synchronized
    fun clearConnections() {
        Log.d(TAG, "clear all snmp connections")
        for (snmpConnection in snmpConnectionPool.values) {
            snmpConnection.close()
        }
    }

    /**
     * remove a v1Connection of the v1Connection pool
     * NOTE: never call this method on main thread!
     *
     * @param deviceConfiguration
     */
    @Synchronized
    fun removeConnection(deviceConfiguration: DeviceConfiguration) {
        Log.d(TAG, "removing connection from pool: " + deviceConfiguration.uniqueDeviceId)
        if (deviceConfiguration.isV3) {
            resetV3Connection(deviceConfiguration, false)
        } else {
            resetV1Connection(deviceConfiguration, false)
        }
    }

    /**
     * face method to call request counter
     */
    fun incrementRequestCounter() {
        SnmpCockpitApp.preferenceManager().incrementRequestCounter()
    }

    private val TAG = SnmpManager::class.java.name
}