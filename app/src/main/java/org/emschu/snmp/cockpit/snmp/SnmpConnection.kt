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

import android.net.TrafficStats
import android.util.Log
import org.emschu.snmp.cockpit.CockpitStateManager
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.cockpitStateManager
import org.emschu.snmp.cockpit.snmp.adapter.V1Adapter
import org.emschu.snmp.cockpit.snmp.adapter.V3Adapter
import org.snmp4j.MessageDispatcher
import org.snmp4j.MessageDispatcherImpl
import org.snmp4j.SNMP4JSettings
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.mp.MPv1
import org.snmp4j.mp.MPv2c
import org.snmp4j.mp.MPv3
import org.snmp4j.mp.SnmpConstants
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
import org.snmp4j.security.SecurityModels
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.security.USM
import org.snmp4j.security.UsmUser
import org.snmp4j.security.nonstandard.PrivAES192With3DESKeyExtension
import org.snmp4j.smi.Integer32
import org.snmp4j.smi.OID
import org.snmp4j.smi.UdpAddress
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.IOException

/**
 * this class represents a real connection to an snmp daemon
 */
open class SnmpConnection(
    val deviceConfiguration: DeviceConfiguration,
    private var transport: DefaultUdpTransportMapping? = null,
) {

    /**
     * direct access should be avoided
     *
     * @return
     */
    protected var snmp: Snmp? = null
        private set
    private var dispatcher: MessageDispatcher? = null
    private var adapter: AbstractSnmpAdapter? = null

    /**
     * init for single connections, generic init is done in Companion class
     */
    init {
        // load generic adapter stuff
        loadAdapters()
        Log.d(TAG, "using local engine id: ${cockpitStateManager.localEngineId}")
        if (transport == null || snmp == null) {
            Log.e(TAG, "no valid transport object")
        }
        if (!deviceConfiguration.isDummy) {
            startListening()
            Log.d(TAG, "is listening: " + transport?.isListening)
        }
    }

    /**
     * the transport instance
     *
     * @return
     */
    fun getTransport(): TransportMapping<UdpAddress>? {
        return transport
    }

    /**
     * checks if a device configuration is registered in this connection
     *
     * @param deviceConfiguration
     * @return
     */
    private fun isRegistered(deviceConfiguration: DeviceConfiguration): Boolean {
        if (adapter != null && (adapter!!.deviceConfiguration.uniqueDeviceId == deviceConfiguration.uniqueDeviceId)) {
            return true
        }
        Log.e(TAG, "requested device configuration not registered!")
        return false
    }

    /**
     * query a single oid
     *
     * @param deviceConfiguration
     * @param oid
     * @return
     */
    fun querySingle(deviceConfiguration: DeviceConfiguration, oid: OID): List<QueryResponse> {
        if (!isRegistered(deviceConfiguration)) {
            Log.e(TAG, "device configuration not registered during querySingle call")
            return emptyList()
        }
        if (!isSnmpAllowed) {
            Log.w(TAG, "no snmp connection is allowed")
            return emptyList()
        }
        return adapter!!.querySingle(oid.toDottedString())
    }

    /**
     * query walk
     *
     * @param deviceConfiguration
     * @param oid
     * @return
     */
    fun queryWalk(deviceConfiguration: DeviceConfiguration, oid: OID): List<QueryResponse> {
        if (!isRegistered(deviceConfiguration)) {
            Log.e(TAG, "device configuration not registered during queryWalk call")
            return emptyList()
        }
        if (!isSnmpAllowed) {
            Log.w(TAG, "no snmp connection is allowed")
            return emptyList()
        }
        return adapter!!.queryWalk(oid.toDottedString())
    }

    /**
     * helper method to check if snmp connections are allowed in general at this very moment
     *
     * @return
     */
    private val isSnmpAllowed: Boolean
        get() {
            val isNetworkAvailable = SnmpCockpitApp.isConnectedToWifi()
            return isNetworkAvailable
        }

    /**
     * closes transport + snmp
     */
    fun close() {
        Log.d(TAG, "stop listening on: " + transport?.listenAddress)
        try {
            snmp?.close()
            transport?.close()
            dispatcher?.stop()
            dispatcher = null
            snmp = null
        } catch (e: IOException) {
            Log.e(SnmpConnection::class.java.name, "exception message: " + e.message)
        } catch (e: RuntimeException) {
            Log.e(SnmpConnection::class.java.name, "exception message: " + e.message)
        } finally {
            Log.d(TAG, "closing snmp connection finished")
        }
    }

    /**
     * load correct adapter depending on version
     */
    @Synchronized
    private fun loadAdapters() {
        startupSnmp()
        if (snmp == null) {
            return
        }
        // fill adapter and target list
        val snmpAdapter: AbstractSnmpAdapter
        when (deviceConfiguration.snmpVersion) {
            DeviceConfiguration.SNMP_VERSION.v1 -> {
                snmpAdapter = V1Adapter(deviceConfiguration, snmp!!, transport as TransportMapping<UdpAddress>, true)
            }

            DeviceConfiguration.SNMP_VERSION.v2c -> {
                snmpAdapter = V1Adapter(deviceConfiguration, snmp!!, transport as TransportMapping<UdpAddress>, false)
            }

            DeviceConfiguration.SNMP_VERSION.v3 -> {
                val userList: MutableList<UsmUser> = mutableListOf()
                snmpAdapter = V3Adapter(deviceConfiguration, snmp!!, transport as TransportMapping<UdpAddress>)
                val user = snmpAdapter.user
                if (user != null) {
                    userList.add(user)
                } else {
                    Log.e(TAG, "invalid user configuration detected")
                }
                // setup usm
                if (userList.isNotEmpty()) {
                    setupUsm(userList)
                }
            }
        }
        adapter = snmpAdapter
    }

    /**
     * init transport + snmp class of this connection
     */
    private fun startupSnmp() {
        if (transport == null) {
            try {
                TrafficStats.setThreadStatsTag(10000)
                transport = DefaultUdpTransportMapping(UdpAddress("0.0.0.0/0"), false)
                transport?.isAsyncMsgProcessingSupported = false
                transport?.socketTimeout = 10000
            } catch (e: IOException) {
                Log.e(TAG, "exception during transport startup: " + e.message)
            } catch (re: RuntimeException) {
                Log.e(TAG, "runtime exception during snmp startup: " + re.message)
            }
        }
        if (dispatcher == null) {
            dispatcher = messageDispatcher
        }
        snmp = Snmp(dispatcher, transport)
    }

    /**
     * configure snmp4j message dispatcher
     *
     * @return
     */
    private val messageDispatcher: MessageDispatcher
        get() {
            if (dispatcher == null) {
                dispatcher = MessageDispatcherImpl()
                dispatcher!!.addMessageProcessingModel(MPv1())
                dispatcher!!.addMessageProcessingModel(MPv2c())
                dispatcher!!.addMessageProcessingModel(MPv3())
            }
            return dispatcher!!
        }

    /**
     * simulates a "ping"
     *
     * @return
     */
    @Synchronized
    fun canPing(deviceConfiguration: DeviceConfiguration): Boolean {
        if (deviceConfiguration.lastPingTime != 0L && (System.currentTimeMillis() - deviceConfiguration.lastPingTime < 5000L)) {
//            Log.d(TAG, "ping request not needed, there is a recent one")
            return true
        }
        val responseList = try {
            querySingle(deviceConfiguration, SnmpConstants.sysName)
        } catch (ex: NoSnmpResponseException) {
            SnmpCockpitApp.deviceManager.registerUnreachable(deviceConfiguration)
            return false
        }
        val canPing = responseList.isNotEmpty()
        val listContentString = responseList.joinToString(", ") { it.oid + ": " + it.value }
        if (canPing) {
            Log.d(TAG, "ping successful")
            if (!checkResponseList(responseList)) {
                Log.w(
                    TAG, "ping: false - snmp connection error. [$listContentString]"
                )
            } else {
                // successful ping
                deviceConfiguration.lastPingTime = System.currentTimeMillis()
                SnmpCockpitApp.deviceManager.registerReachable(deviceConfiguration)
                return true
            }
        } else {
            Log.d(TAG, "ping NOT successful. responseList (should be sysName): $listContentString")
        }
        SnmpCockpitApp.deviceManager.registerUnreachable(deviceConfiguration)
        return false
    }

    /**
     * check for wrong user
     *
     * @param responseList
     * @return
     */
    private fun checkResponseList(responseList: List<QueryResponse>): Boolean {
        responseList.forEach { response ->
            if (response.variableBinding.oid == SnmpConstants.sysName) {
                return true
            }
        }
        return false
    }

    /**
     * checks transport is listening
     */
    fun startListening() {
        try {
            if (transport?.isListening != true) {
                snmp?.listen()
                Log.d(TAG, "transport starts listening on udp socket: " + transport?.listenAddress.toString())
                return
            }
        } catch (ioException: IOException) {
            Log.e(TAG, "exception with connection socket: " + ioException.message)
        }
    }

    /**
     * helper method to control usm
     *
     * @param userList
     */
    private fun setupUsm(userList: List<UsmUser>) {
        Log.d(TAG, "usm handling enabled - v3 connection exists")
        if (usm != null) {
            Log.d(TAG, "unregistering old security model")
            SecurityModels.getInstance()
                .removeSecurityModel(Integer32(usm!!.id))
        }
        usm = USM(SecurityProtocols.getInstance(), CockpitStateManager.localEngineId, engineBoots++)
        Log.d(TAG, "engine boots: $engineBoots")
        for (singleUser in userList) {
            usm?.addUser(singleUser)
        }
        Log.d(TAG, "usm: added " + userList.size + " users")
        SecurityModels.getInstance()
            .addSecurityModel(usm)
    }

    companion object {
        private val TAG = SnmpConnection::class.java.name
        private var usm: USM? = null
        private var engineBoots = -1

        init {
            snmp4JSettings()
        }

        /**
         * this is executed only once per app lifetime
         */
        private fun snmp4JSettings() {
            Log.d(TAG, "snmp4j is initially configured")
            SNMP4JSettings.setAllowSNMPv2InV1(true)
            // https://www.iana.org/assignments/enterprise-numbers/enterprise-numbers
            SNMP4JSettings.setEnterpriseID(696)
            SNMP4JSettings.setMaxEngineIdCacheSize(100000) // 4 MB
            SNMP4JSettings.setThreadJoinTimeout(30000)
            SNMP4JSettings.setSnmp4jStatistics(SNMP4JSettings.Snmp4jStatistics.basic)
            SNMP4JSettings.setCheckUsmUserPassphraseLength(true)
            SNMP4JSettings.setForwardRuntimeExceptions(false)
            SNMP4JSettings.setExtensibilityEnabled(false)
            SecurityProtocols.getInstance().addDefaultProtocols()
            SecurityProtocols.getInstance().addPrivacyProtocol(Priv3DES())

            val instance = SecurityProtocols.getInstance()
            instance.addPrivacyProtocol(PrivDES())
            instance.addPrivacyProtocol(Priv3DES())
            instance.addPrivacyProtocol(PrivAES128())
            instance.addPrivacyProtocol(PrivAES192())
            instance.addPrivacyProtocol(PrivAES256())
            instance.addPrivacyProtocol(PrivAES192With3DESKeyExtension())
            instance.addAuthenticationProtocol(AuthSHA())
            instance.addAuthenticationProtocol(AuthHMAC128SHA224())
            instance.addAuthenticationProtocol(AuthHMAC192SHA256())
            instance.addAuthenticationProtocol(AuthHMAC256SHA384())
            instance.addAuthenticationProtocol(AuthHMAC384SHA512())
            instance.addAuthenticationProtocol(AuthMD5())
        }
    }
}