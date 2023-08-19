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
package org.emschu.snmp.cockpit.snmp.adapter

import android.util.Log
import org.emschu.snmp.cockpit.snmp.AbstractSnmpAdapter
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.NoSnmpResponseException
import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.snmp4j.AbstractTarget
import org.snmp4j.PDU
import org.snmp4j.ScopedPDU
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.UserTarget
import org.snmp4j.security.SecurityLevel
import org.snmp4j.security.UsmUser
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import org.snmp4j.util.DefaultPDUFactory
import org.snmp4j.util.TreeUtils

/**
 * this class handles snmp4j version specific stuff for snmpv3 connections
 */
class V3Adapter(
    deviceConfiguration: DeviceConfiguration, snmp: Snmp, udpAddressTransportMapping: TransportMapping<UdpAddress>,
) : AbstractSnmpAdapter(snmp, deviceConfiguration, udpAddressTransportMapping) {

    override fun buildTarget(): AbstractTarget<UdpAddress> {
        if (target != null) {
            return target!!
        }
        val userTarget = UserTarget<UdpAddress>()
        val secLevel = deviceConfiguration.securityLevel.snmpValue
        Log.d(TAG, "using security level: " + SecurityLevel.values()[secLevel])
        userTarget.securityLevel = secLevel
        userTarget.securityName = OctetString(deviceConfiguration.username)
        userTarget.address = UdpAddress(udpAddress)
        userTarget.version = deviceConfiguration.snmpVersion.number
        userTarget.retries = deviceConfiguration.retries
        userTarget.timeout = deviceConfiguration.timeout.toLong()
        target = userTarget
        return userTarget
    }

    /**
     * specific snmpv3 method for usm user entry handling
     *
     * @return
     */
    val user: UsmUser?
        get() {
            val authPassphrase = OctetString(deviceConfiguration.authPassphrase)
            val privPassphrase = OctetString(deviceConfiguration.privacyPassphrase)
            val securityName = OctetString(deviceConfiguration.username)
            val securityLevel = deviceConfiguration.securityLevel
            Log.d(TAG, "security level: $securityLevel")
            return when (securityLevel) {
                SecurityLevel.authPriv -> {
                    if (authPassphrase.length() < 8) {
                        Log.w(TAG, "auth passphrase is too short. invalid by rfc.")
                        return null
                    }
                    if (privPassphrase.length() < 8) {
                        Log.w(TAG, "priv passphrase is too short. invalid by rfc.")
                        return null
                    }
                    UsmUser(
                        securityName,
                        deviceConfiguration.authProtocol,
                        authPassphrase,
                        deviceConfiguration.privProtocol,
                        privPassphrase
                    )
                }

                SecurityLevel.authNoPriv -> {
                    if (authPassphrase.length() < 8) {
                        null
                    } else UsmUser(
                        securityName, deviceConfiguration.authProtocol, authPassphrase, null, null
                    )
                }

                SecurityLevel.undefined, SecurityLevel.noAuthNoPriv -> UsmUser(
                    securityName, null, null, null, null
                )
            }
        }

    override fun querySingle(oid: String?): List<QueryResponse> {
        Log.d(TAG, "query single: $oid")
        val pdu = ScopedPDU()
        pdu.add(VariableBinding(OID(oid)))
        pdu.contextName = OctetString(deviceConfiguration.context)
        pdu.type = PDU.GET
        return basicSingleGet(pdu)
    }

    override fun queryWalk(oid: String?): List<QueryResponse> {
        if (oid == null) {
            return emptyList()
        }
        val treeUtils = TreeUtils(snmp, DefaultPDUFactory(PDU.GETBULK))
        return try {
            basicWalk(treeUtils, oid)
        } catch (e: NoSnmpResponseException) {
            Log.w(TAG, "no snmp v3 response for oid $oid")
            emptyList()
        }
    }

    companion object {
        private val TAG = V3Adapter::class.java.name
    }
}