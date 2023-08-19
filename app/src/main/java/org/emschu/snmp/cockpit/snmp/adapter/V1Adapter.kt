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
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.security.SecurityLevel
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import org.snmp4j.util.DefaultPDUFactory
import org.snmp4j.util.TreeUtils

/**
 * this class handles snmp v1 and v2c specific stuff
 */
class V1Adapter(
    deviceConfiguration: DeviceConfiguration,
    snmp: Snmp,
    udpAddressTransportMapping: TransportMapping<UdpAddress>,
    val isV1: Boolean,
) : AbstractSnmpAdapter(
    snmp, deviceConfiguration, udpAddressTransportMapping
) {
    override fun buildTarget(): AbstractTarget<UdpAddress> {
        if (target != null) {
            return target!!
        }
        val communityTarget = CommunityTarget<UdpAddress>()
        val securityName = OctetString(deviceConfiguration.username)
        communityTarget.community = securityName
        communityTarget.securityLevel = SecurityLevel.NOAUTH_NOPRIV // this is fix for v1!
        communityTarget.address = UdpAddress(udpAddress)
        communityTarget.version = deviceConfiguration.snmpVersion.number
        communityTarget.retries = deviceConfiguration.retries
        communityTarget.timeout = deviceConfiguration.timeout.toLong()
        target = communityTarget
        return communityTarget
    }

    override fun querySingle(oid: String?): List<QueryResponse> {
        Log.d(TAG, "query single: $oid")
        val pdu: PDU = if (isV1) {
            DefaultPDUFactory.createPDU(1)
        } else {
            DefaultPDUFactory.createPDU(2)
        }
        pdu.add(VariableBinding(OID(oid)))
        pdu.type = PDU.GETNEXT
        return basicSingleGet(pdu)
    }

    override fun queryWalk(oid: String?): List<QueryResponse> {
        if (oid == null) {
            return emptyList()
        }
        val treeUtils = TreeUtils(snmp, DefaultPDUFactory())
        return try {
            basicWalk(treeUtils, oid)
        } catch (e: NoSnmpResponseException) {
            Log.w(TAG, "no snmp v1 response for oid $oid")
            emptyList()
        }
    }

    companion object {
        private val TAG = V1Adapter::class.java.name
    }
}