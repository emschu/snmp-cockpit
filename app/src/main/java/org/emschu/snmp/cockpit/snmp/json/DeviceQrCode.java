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

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.emschu.snmp.cockpit.snmp.SnmpEndpoint;
import org.jetbrains.annotations.NotNull;

/**
 * this class represents a device qr code and is used by jackson json databind to encode qr code input
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "user",
        "pw",
        "enc",
        "naddr"
})
public class DeviceQrCode {

    @JsonProperty("user")
    private String user;
    @JsonProperty("pw")
    private String pw;
    @JsonProperty("enc")
    private String enc;
    @JsonProperty("naddr")
    private QrCodeAddressPart naddr;

    @JsonProperty("user")
    public String getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(String user) {
        this.user = user;
    }

    @JsonProperty("pw")
    public String getPw() {
        return pw;
    }

    @JsonProperty("pw")
    public void setPw(String pw) {
        this.pw = pw;
    }

    @JsonProperty("enc")
    public String getEnc() {
        return enc;
    }

    @JsonProperty("enc")
    public void setEnc(String enc) {
        this.enc = enc;
    }

    @JsonProperty("naddr")
    public QrCodeAddressPart getNaddr() {
        return naddr;
    }

    @JsonProperty("naddr")
    public void setNaddr(QrCodeAddressPart naddr) {
        this.naddr = naddr;
    }

    public boolean hasIpv4Port() {
        return naddr.getIPv4().contains(":");
    }

    public boolean hasIpv6Port() {
        return naddr.getIPv6().contains("]:");
    }

    public int getPortv4() {
        try {
            if (hasIpv4Port()) {
                return Integer.parseInt(naddr.getIPv4().split(":")[1]);
            }
        } catch (NumberFormatException nfe) {
            return 161;
        }
        return 161;
    }

    public int getPortv6() {
        try {
            if (hasIpv6Port()) {
                return Integer.parseInt(naddr.getIPv6().split("\\]:")[1]);
            }
        } catch (NumberFormatException nfe) {
            return 161;
        }
        return 161;
    }

    @Nullable
    public SnmpEndpoint getEndpoint() {
        if (naddr.getIPv4() == null || naddr.getIPv6() == null) {
            return null;
        }
        if (naddr.getIPv4().isEmpty() && naddr.getIPv6().isEmpty()) {
            return null;
        }
        if (!naddr.getIPv4().isEmpty()) {
            String addr;
            if (hasIpv4Port()) {
                addr = naddr.getIPv4().split(":")[0];
            } else {
                addr = naddr.getIPv4();
            }
            return new SnmpEndpoint(addr, false, getPortv4());
        }
        if (!naddr.getIPv6().isEmpty()) {
            String addr;
            if (hasIpv6Port()) {
                // strip first "["
                addr = naddr.getIPv6().split("\\]:")[0].substring(1);
            } else {
                addr = naddr.getIPv6();
            }
            return new SnmpEndpoint(addr, true, getPortv6());
        }
        return null;
    }

    @NotNull
    @Override
    public String toString() {
        return "DeviceQrCode{" +
                "user='" + user + '\'' +
                ", pw='" + pw + '\'' +
                ", enc='" + enc + '\'' +
                ", naddr=" + naddr +
                '}';
    }
}

