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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
import org.snmp4j.security.SecurityLevel
import org.snmp4j.security.nonstandard.PrivAES192With3DESKeyExtension
import org.snmp4j.smi.OID

/**
 * This class represents a real world connection to a specific snmp service
 */
@Parcelize
data class DeviceConfiguration(
    val snmpVersion: SNMP_VERSION,
    val isIpv6: Boolean = false,
    val targetIp: String = "",
    val targetPort: Int = 161,
    val networkProtocol: String = "udp",
    // note: only the following combination seems to work with open bsd snmp!
    val authProtocol: OID? = AuthSHA.ID,
    val privProtocol: OID? = PrivAES128.ID,
    val securityLevel: SecurityLevel = SecurityLevel.undefined,
    val context: String = "",
    val retries: Int = 2,
    val timeout: Int = 5000,

    // user and privacy data:
    val username: String = "",
    val authPassphrase: String = "",
    val privacyPassphrase: String = "",
    val isDummy: Boolean = false,
    val isOnline: Boolean = true,
    var lastPingTime: Long = 0,
) : Parcelable {
    /**
     * unique for a connection to a single device (= ip + port + username/community + snmp version)
     *
     * @return
     */
    val uniqueDeviceId: String
        get() = "$username-$targetIp-$targetPort-${snmpVersion.ordinal}"

    /**
     * label to display in lists
     *
     * if a null sysName is provided the ip is used
     *
     * @param sysName
     * @return
     */
    fun getListLabel(sysName: String?): String {
        val sb = StringBuilder()
        sb.append(username.ifBlank { "<unknown>" })
            .append("@")

        if (sysName == null) {
            if (isIpv6 && targetPort != 161) {
                sb.append("[")
                    .append(targetIp)
                    .append("]")
            } else {
                sb.append(targetIp.ifBlank { "unknown-host" })
            }
        } else {
            sb.append(sysName.ifBlank { "unknown-host" })
        }
        // only show port, if its special
        if (targetPort != 161) {
            sb.append(":")
                .append(targetPort)
        }
        return sb.toString()
    }

    val isV1: Boolean
        get() = snmpVersion == SNMP_VERSION.v1
    val isV2c: Boolean
        get() = snmpVersion == SNMP_VERSION.v2c
    val isV3: Boolean
        get() = snmpVersion == SNMP_VERSION.v3

    /**
     * info text about connection details
     * should not be translated
     * @return
     */
    val connectionDetailsText: String
        get() {
            val sb = StringBuilder()
            sb.append("SNMP-Version: ")
                .append(snmpVersion)
                .append("\n")
            sb.append("IP: ")
                .append(targetIp)
                .append("\n")
            sb.append("Port: ")
                .append(targetPort)
                .append("\n")
            if (isV3) {
                sb.append("AuthProtocol: ")
                    .append(authProtocolLabel)
                    .append(" (OID: ")
                    .append(authProtocol)
                    .append(")\n")
                sb.append("PrivProtocol: ")
                    .append(privProtocolLabel)
                    .append(" (OID: ")
                    .append(privProtocol)
                    .append(")\n\n")
            }
            sb.append("Retries: ")
                .append(retries)
                .append("\n")
            sb.append("Timeout: ")
                .append(timeout)
                .append(" ms")
                .append("\n\n")
            sb.append("SecurityLevel: ")
                .append(securityLevel.toString())
                .append("\n")
            if (isV3) {
                sb.append("User: ")
            } else {
                sb.append("Community: ")
            }
            sb.append(username)
                .append("\n")
            if (isV3 && context.isNotBlank()) {
                sb.append("Context: ")
                    .append(context)
                    .append("\n")
            }
            return sb.toString()
        }

    /**
     * auth protocol label
     * @return
     */
    val authProtocolLabel: String?
        get() {
            if (AuthSHA.ID.equals(authProtocol)) {
                return "SHA-1"
            }
            if (AuthMD5.ID.equals(authProtocol)) {
                return "MD5"
            }
            if (AuthHMAC128SHA224.ID.equals(authProtocol)) {
                return "SHA-224"
            }
            if (AuthHMAC192SHA256.ID.equals(authProtocol)) {
                return "SHA-256"
            }
            if (AuthHMAC256SHA384.ID.equals(authProtocol)) {
                return "SHA-384"
            }
            return if (AuthHMAC384SHA512.ID.equals(authProtocol)) {
                "SHA-512"
            } else null
        }

    /**
     * get human readable label of privProtocol
     *
     * @return
     */
    val privProtocolLabel: String?
        get() {
            if (PrivAES128.ID.equals(privProtocol)) {
                return "AES-128"
            }
            if (PrivDES.ID.equals(privProtocol)) {
                return "DES"
            }
            if (Priv3DES.ID.equals(privProtocol)) {
                return "Triple DES"
            }
            if (PrivAES192.ID.equals(privProtocol)) {
                return "AES-192"
            }
            if (PrivAES192With3DESKeyExtension.ID.equals(privProtocol)) {
                return "AES-192+DES"
            }
            return if (PrivAES256.ID.equals(privProtocol)) {
                "AES-256"
            } else null
        }

    override fun toString(): String {
        return "DeviceConfiguration{snmpVersion=$snmpVersion, targetIp='$targetIp', targetPort=$targetPort, " +
            "networkProtocol='$networkProtocol', authProtocol=$authProtocol, privProtocol=$privProtocol, " +
            "retries=$retries, timeout=$timeout, username='$username', authPassphrase='xxx', " +
            "privacyPassphrase='xxx', isDummy=$isDummy, isIpv6=$isIpv6, lastPingTime=$lastPingTime}"
    }

    /**
     * enum of supported snmp versions, number needs to correspond to snmp4j integer representations of snmp versions
     */
    enum class SNMP_VERSION(val number: Int) {
        v1(1), v2c(2), v3(3)
    }
}

@Parcelize
class DeviceConfigurationList(val deviceConfigurations: List<DeviceConfiguration>) : Parcelable