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
import org.emschu.snmp.cockpit.BuildConfig
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.cockpitStateManager
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.snmpManager
import org.snmp4j.AbstractTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.event.ResponseEvent
import org.snmp4j.smi.OID
import org.snmp4j.smi.UdpAddress
import org.snmp4j.util.TreeUtils
import java.io.IOException
import java.util.*

/**
 * this adapter abstracts the (partially) version specific device/message handling
 *
 *
 */
abstract class AbstractSnmpAdapter(
    var snmp: Snmp,
    val deviceConfiguration: DeviceConfiguration,
    protected var udpAddressTransportMapping: TransportMapping<UdpAddress>,
) {
    @JvmField
    protected var target: AbstractTarget<UdpAddress>? = null

    // abstract methods
    /**
     * define version specific target in this class
     *
     * @return
     */
    abstract fun buildTarget(): AbstractTarget<UdpAddress>

    /**
     * method to query a single oid
     *
     * @param oid
     * @return
     */
    abstract fun querySingle(oid: String?): List<QueryResponse>

    /**
     * method to query a walk of a single oid
     *
     * @param oid
     * @return
     */
    abstract fun queryWalk(oid: String?): List<QueryResponse>

    /**
     * common used methods
     *
     * @param treeUtils
     * @param oid
     * @return
     * @throws NoSnmpResponseException
     */
    @Throws(NoSnmpResponseException::class)
    fun basicWalk(treeUtils: TreeUtils, oid: String): List<QueryResponse> {
        Log.d(TAG, "query walk - $oid")
        if (!isSnmpConnectionAllowed) {
            Log.w(TAG, "no snmp connection allowed, execution prevented")
            return emptyList()
        }
        if (deviceConfiguration.isDummy) {
            return emptyList()
        }
        if (!udpAddressTransportMapping.isListening) {
            Log.wtf(TAG, "no transport mapping is listening!")
            return emptyList()
        }
        val events = Collections.synchronizedList(treeUtils.getSubtree(buildTarget(), OID(oid)))
        if (events.isEmpty()) {
            Log.w(TAG, "no response event returned")
            throw NoSnmpResponseException()
        }
        Log.d(TAG, String.format("request returned %s responses", events.size))
        synchronized(events) {
            val queryResponses = mutableListOf<QueryResponse>()
            // this code is critical for ConcurrentModificationExceptions
            for (event in events) {
                if (event == null) {
                    continue
                }
                if (event.isError) {
                    Log.e(TAG, "Error: table OID [" + oid + "] " + event.errorMessage)
                    continue
                }
                val varBindings = event.variableBindings ?: continue
                for (varBinding in varBindings) {
                    if (varBinding != null) {
                        queryResponses.add(QueryResponse(varBinding.oid.toDottedString(), varBinding))
                    }
                }
            }
            snmpManager.incrementRequestCounter()
            return queryResponses
        }
    }

    /**
     * synchronous get
     *
     * @param pdu
     * @return
     */
    @Throws(NoSnmpResponseException::class)
    fun basicSingleGet(pdu: PDU?): List<QueryResponse> {
        if (!isSnmpConnectionAllowed) {
            Log.e(TAG, "no snmp connection allowed")
            return emptyList()
        }
        try {
            if (deviceConfiguration.isDummy) {
                return emptyList()
            }
            if (!udpAddressTransportMapping.isListening) {
                Log.w(TAG, "Socket is no longer open!")
                return emptyList()
            }
            val re: ResponseEvent<*> = snmp.get(pdu, buildTarget())
            val isResponseValid = checkResponseEvent(re)
            if (isResponseValid) {
                snmpManager.incrementRequestCounter()
                val responseList: MutableList<QueryResponse> = mutableListOf()
                for (varBind in re.response.variableBindings) {
                    responseList.add(QueryResponse(varBind.oid.toDottedString(), varBind))
                }
                return responseList
            }
        } catch (e: IOException) {
            if (e.message != null) {
                Log.w(AbstractSnmpAdapter::class.simpleName, "io-exception message: " + e.message)
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
        } catch (e: NoSnmpResponseException) {
            if (e.message != null) {
                Log.w(AbstractSnmpAdapter::class.simpleName, "exception message: " + e.message)
            }
        }
        return emptyList()
    }

    /**
     * helper method to check if we are allowed to fire a snmp request
     *
     * @return
     */
    private val isSnmpConnectionAllowed: Boolean
        get() {
            if (cockpitStateManager.networkAvailabilityObservable.value == false) {
                Log.w(TAG, "request not allowed. network not secure!")
                return false
            }
            return true
        }

    /**
     * generic method to check if response event is valid
     *
     * @param re ResponseEvent
     * @return result
     * @throws NoSnmpResponseException
     */
    @Throws(NoSnmpResponseException::class)
    protected fun checkResponseEvent(re: ResponseEvent<*>?): Boolean {
        if (re != null) {
            val pdu = re.response ?: throw NoSnmpResponseException()
            Log.d(TAG, "pdu status: " + pdu.errorStatus + " " + pdu.errorStatusText)
            return pdu.errorStatus == PDU.noError
        }
        return false
    }

    /**
     * formats the address string for a connection
     *
     * @return
     */
    protected val genericAddress: String
        get() = String.format(
            "%s:%s/%s",
            deviceConfiguration.networkProtocol,
            deviceConfiguration.targetIp,
            deviceConfiguration.targetPort
        )
    protected val udpAddress: String
        get() = if (deviceConfiguration.isIpv6) {
            String.format(
                "[%s]/%s", deviceConfiguration.targetIp, deviceConfiguration.targetPort
            )
        } else String.format(
            "%s/%s", deviceConfiguration.targetIp, deviceConfiguration.targetPort
        )

    companion object {
        private val TAG = AbstractSnmpAdapter::class.java.name
    }
}