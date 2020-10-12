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

package org.emschu.snmp.cockpit.snmp.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.jetbrains.annotations.NotNull;

/**
 * representing "nadr" object of device qr code
 * using jackson databind 2 annotations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "IPv4",
        "IPv6"
})
public class QrCodeAddressPart {
    @JsonProperty("IPv4")
    private String iPv4;
    @JsonProperty("IPv6")
    private String iPv6;

    @JsonProperty("IPv4")
    public String getIPv4() {
        return iPv4;
    }

    @JsonProperty("IPv4")
    public void setIPv4(String iPv4) {
        this.iPv4 = iPv4;
    }

    @JsonProperty("IPv6")
    public String getIPv6() {
        return iPv6;
    }

    @JsonProperty("IPv6")
    public void setIPv6(String iPv6) {
        this.iPv6 = iPv6;
    }

    @NotNull
    @Override
    public String toString() {
        return "QrCodeAddressPart{" +
                "iPv4='" + iPv4 + '\'' +
                ", iPv6='" + iPv6 + '\'' +
                '}';
    }
}
