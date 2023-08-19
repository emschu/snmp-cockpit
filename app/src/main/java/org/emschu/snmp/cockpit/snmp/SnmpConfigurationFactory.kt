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

import org.emschu.snmp.cockpit.util.DeviceAuthEncodingParser.decodeString
import org.snmp4j.security.SecurityLevel

/**
 * factory class for a [DeviceConfiguration] class ready to be used with the [SnmpConnection] class
 */
class SnmpConfigurationFactory {
    fun createSnmpV1Config(
        targetIp: String,
        community: String,
        targetPort: Int,
        isIpv6: Boolean = false,
        timeout: Int = 5000,
        retries: Int = 2,
    ): DeviceConfiguration {
        return DeviceConfiguration(
            snmpVersion = DeviceConfiguration.SNMP_VERSION.v1,
            targetPort = targetPort,
            isIpv6 = isIpv6,
            isDummy = false,
            targetIp = targetIp,
            username = community,
            authProtocol = null,
            privProtocol = null,
            timeout = timeout,
            retries = retries
        )
    }

    /**
     * v3 config
     *
     * @param targetIp
     * @param userName
     * @param password
     * @param encPhrase
     * @return
     */
    fun createSnmpV3Config(
        targetIp: String,
        userName: String,
        password: String,
        encPhrase: String,
        targetPort: Int,
        isIpv6: Boolean = false,
        timeout: Int = 5000,
        retries: Int = 2,
    ): DeviceConfiguration {
        val encodingStringPartial = decodeString(encPhrase)
        val dc = DeviceConfiguration(
            snmpVersion = DeviceConfiguration.SNMP_VERSION.v3,
            targetIp = targetIp,
            isIpv6 = isIpv6,
            targetPort = targetPort,
            username = userName,
            authPassphrase = password,
            authProtocol = encodingStringPartial.authProtocol?.authOID,
            privProtocol = encodingStringPartial.privProtocol?.privOID,
            securityLevel = encodingStringPartial.securityLevel?.snmp4jSecLevel ?: SecurityLevel.undefined,
            context = encodingStringPartial.context ?: "",
            privacyPassphrase = encodingStringPartial.password ?: "",
            timeout = timeout,
            retries = retries,
        )
        return dc
    }
}