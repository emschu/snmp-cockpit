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
package org.emschu.snmp.cockpit.snmp.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.emschu.snmp.cockpit.snmp.SnmpEndpoint

/**
 * this class represents a device qr code and is used by jackson json databind to encode qr code input
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("user", "pw", "enc", "naddr")
class DeviceQrCode() {
    // NOTE: jackson needs a no-args constructor
    constructor(user: String, naddr: QrCodeAddressPart = QrCodeAddressPart()) : this() {
        this.user = user
        this.naddr = naddr
    }

    @get:JsonProperty("user")
    @set:JsonProperty("user")
    var user: String? = null

    @get:JsonProperty("pw")
    @set:JsonProperty("pw")
    var pw: String? = null

    @get:JsonProperty("enc")
    @set:JsonProperty("enc")
    var enc: String? = null

    @get:JsonProperty("naddr")
    @set:JsonProperty("naddr")
    var naddr: QrCodeAddressPart? = null

    @JsonIgnore
    fun hasIpv4Port(): Boolean {
        return naddr?.iPv4?.contains(":") ?: false
    }

    @JsonIgnore
    fun hasIpv6Port(): Boolean {
        return naddr?.iPv6?.contains("]:") ?: false
    }

    val portv4: Int
        get() {
            try {
                if (naddr != null && hasIpv4Port()) {
                    return (naddr!!.iPv4 ?: "").split(":".toRegex())
                        .toTypedArray()[1].toInt()
                }
            } catch (nfe: NumberFormatException) {
                return 161
            }
            return 161
        }
    val portv6: Int
        get() {
            try {
                if (naddr != null && hasIpv6Port()) {
                    return (naddr!!.iPv6 ?: "").split("\\]:".toRegex())
                        .toTypedArray()[1].toInt()
                }
            } catch (nfe: NumberFormatException) {
                return 161
            }
            return 161
        }

    val isSnmpv3: Boolean
        get() = !pw.isNullOrBlank() && !enc.isNullOrBlank()

    // strip first "["
    val endpoint: SnmpEndpoint?
        get() {
            if (naddr == null) {
                return null
            }
            if (naddr!!.iPv4 == null || naddr!!.iPv6 == null) {
                return null
            }
            if ((naddr!!.iPv4 ?: "").isBlank() && (naddr!!.iPv6 ?: "").isBlank()) {
                return null
            }
            if ((naddr!!.iPv4 ?: "").isNotBlank()) {
                val addr: String = if (hasIpv4Port()) {
                    (naddr!!.iPv4 ?: "").split(":".toRegex())
                        .toTypedArray()[0]
                } else {
                    naddr!!.iPv4 ?: ""
                }
                return SnmpEndpoint(addr, false, portv4)
            }
            if ((naddr!!.iPv6 ?: "").isNotBlank()) {
                val addr: String = if (hasIpv6Port()) {
                    // strip first "["
                    (naddr!!.iPv6 ?: "").split("\\]:".toRegex())
                        .toTypedArray()[0].substring(1)
                } else {
                    naddr!!.iPv6 ?: ""
                }
                return SnmpEndpoint(addr, true, portv6)
            }
            return null
        }
}