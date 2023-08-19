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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * representing "nadr" object of device qr code
 * using jackson databind 2 annotations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("IPv4", "IPv6")
class QrCodeAddressPart(ipv4: String = "", ipv6: String = "") {
    @get:JsonProperty("IPv4")
    @set:JsonProperty("IPv4")
    @JsonProperty("IPv4")
    var iPv4: String? = ipv4

    @get:JsonProperty("IPv6")
    @set:JsonProperty("IPv6")
    @JsonProperty("IPv6")
    var iPv6: String? = ipv6

    override fun toString(): String {
        return "QrCodeAddressPart{" + "iPv4='" + iPv4 + '\'' + ", iPv6='" + iPv6 + '\'' + '}'
    }
}