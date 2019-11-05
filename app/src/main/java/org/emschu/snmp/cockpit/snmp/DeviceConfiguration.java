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

package org.emschu.snmp.cockpit.snmp;


import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthHMAC128SHA224;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthHMAC256SHA384;
import org.snmp4j.security.AuthHMAC384SHA512;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.nonstandard.PrivAES192With3DESKeyExtension;
import org.snmp4j.smi.OID;

import org.emschu.snmp.cockpit.activity.SNMPLoginActivity;

/**
 * A wrapper around everything with snmp4j we need to establish a collection
 */
public class DeviceConfiguration {
    private static final int DEFAULT_TIMEOUT_OFFSET = 5000;

    private SNMP_VERSION snmpVersion = null;
    private String targetIp = "";
    private int targetPort = 161;
    private String networkProtocol = "udp";
    // note: only the following combination seems to work with open bsd snmp!
    private OID authProtocol = AuthSHA.ID;
    private OID privProtocol = PrivAES128.ID;
    private SecurityLevel securityLevel = SecurityLevel.undefined;
    private String context = null;
    private int retries = 2;
    private int timeout = 5000;
    // user and privacy data:
    private String username = "";
    private String authPassphrase = "";
    private String privacyPassphrase = "";
    private boolean isDummy = false;
    private boolean isConnectionTestNeeded = true;
    private boolean isIpv6 = false;
    private long lastPingTime = 0;

    /**
     * empty constructor
     */
    public DeviceConfiguration() {
    }

    /**
     * constructor to clone
     *
     * @param deviceConfiguration
     */
    public DeviceConfiguration(DeviceConfiguration deviceConfiguration) {
        snmpVersion = deviceConfiguration.getSnmpVersionEnum();
        targetIp = deviceConfiguration.getTargetIp();
        targetPort = deviceConfiguration.getTargetPort();
        networkProtocol = deviceConfiguration.getNetworkProtocol();
        authProtocol = deviceConfiguration.getAuthProtocol();
        privProtocol = deviceConfiguration.getPrivProtocol();
        retries = deviceConfiguration.getRetries();
        timeout = deviceConfiguration.getTimeout();
        username = deviceConfiguration.getUsername();
        authPassphrase = deviceConfiguration.getAuthPassphrase();
        privacyPassphrase = deviceConfiguration.getPrivacyPassphrase();
        securityLevel = deviceConfiguration.getSecurityLevel();
        context = deviceConfiguration.getContext();
        isDummy = deviceConfiguration.isDummy();
        isConnectionTestNeeded = deviceConfiguration.isConnectionTestNeeded();
        isIpv6 = deviceConfiguration.isIpv6();
        lastPingTime = deviceConfiguration.getLastPingTime();
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public OID getAuthProtocol() {
        return authProtocol;
    }

    public void setAuthProtocol(OID authProtocol) {
        this.authProtocol = authProtocol;
    }

    public OID getPrivProtocol() {
        return privProtocol;
    }

    public void setPrivProtocol(OID privProtocol) {
        this.privProtocol = privProtocol;
    }

    public String getNetworkProtocol() {
        return networkProtocol;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * VERY important code
     *
     * @return
     */
    public String getUniqueDeviceId() {
        return username + "-" + targetIp + "-" + targetPort + "-" + snmpVersion;
    }

    /**
     * to differentiate between devices and connections we append priv and auth to the device id
     *
     * for v1: connectionId = deviceId
     *
     * @return
     */
    public String getUniqueConnectionId() {
        if (getSnmpVersion() == 3) {
            return getUniqueDeviceId() + "-" + privProtocol.toDottedString() + "-" + authProtocol.toDottedString();
        } else {
            return getUniqueDeviceId();
        }
    }

    /**
     * label to display in lists
     *
     * @param sysName
     * @return
     */
    public String getListLabel(String sysName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getUsername()).append("@");

        // only show port, if its special
        if (isIpv6 ) {
            if (sysName == null) {
                sb.append("[").append(getTargetIp()).append("]");
            } else {
                sb.append(sysName);
            }
        } else {
            if (sysName == null) {
                sb.append(getTargetIp());
            } else {
                sb.append(sysName);
            }
        }

        if (getTargetPort() != SNMPLoginActivity.DEFAULT_SNMP_PORT) {
            sb.append(":").append(getTargetPort());
        }
        return sb.toString();
    }

    /**
     * snmp version ready to be used for snmp4j
     *
     * @return
     */
    public int getSnmpVersion() {
        switch (snmpVersion) {
            case v1:
                return SnmpConstants.version1;
            case v2c:
                return SnmpConstants.version2c;
            case v3:
                return SnmpConstants.version3;
        }
        throw new IllegalStateException("no snmp version information");
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAuthPassphrase() {
        return authPassphrase;
    }

    public void setAuthPassphrase(String authPassphrase) {
        this.authPassphrase = authPassphrase;
    }

    public String getPrivacyPassphrase() {
        return privacyPassphrase;
    }

    public void setPrivacyPassphrase(String privacyPassphrase) {
        this.privacyPassphrase = privacyPassphrase;
    }

    public SNMP_VERSION getSnmpVersionEnum() {
        return snmpVersion;
    }

    public void setSnmpVersion(SNMP_VERSION snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    public boolean isV3() {
        return this.snmpVersion == SNMP_VERSION.v3;
    }

    public boolean isV1() {
        return this.snmpVersion == SNMP_VERSION.v1;
    }

    public boolean isV2c() {
        return this.snmpVersion == SNMP_VERSION.v2c;
    }

    /**
     * info text about connection details
     * should not be translated
     * @return
     */
    public String getConnectionDetailsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("SNMP-Version: ").append(snmpVersion).append("\n");
        sb.append("IP: ").append(targetIp).append("\n");
        sb.append("Port: ").append(targetPort).append("\n");

        String authProtocolLabel = getAuthProtocolLabel();
        String privProtocolLabel = getPrivProtocolLabel();
        if (isV3()) {
            sb.append("AuthProtocol: ").append(authProtocolLabel)
                    .append(" (OID: ").append(authProtocol).append(")\n");
            sb.append("PrivProtocol: ").append(privProtocolLabel)
                    .append(" (OID: ").append(privProtocol).append(")\n\n");
        }

        sb.append("Retries: ").append(retries).append("\n");
        sb.append("Timeout: ").append(timeout).append(" ms").append("\n\n");
        sb.append("SecurityLevel: ").append(securityLevel.toString()).append("\n");

        if (isV3()) {
            sb.append("User: ");
        } else {
            sb.append("Community: ");
        }
        sb.append(username).append("\n");

        if (isV3() && context != null && !context.isEmpty()) {
            sb.append("Context: ").append(context).append("\n");
        }

        return sb.toString();
    }

    /**
     * auth protocol label
     * @return
     */
    public String getAuthProtocolLabel() {
        if (AuthSHA.ID.equals(authProtocol)) {
            return "SHA-1";
        }
        if (AuthMD5.ID.equals(authProtocol)) {
            return "MD5";
        }
        if (AuthHMAC128SHA224.ID.equals(authProtocol)) {
            return "SHA-224";
        }
        if (AuthHMAC192SHA256.ID.equals(authProtocol)) {
            return "SHA-256";
        }
        if (AuthHMAC256SHA384.ID.equals(authProtocol)) {
            return "SHA-384";
        }
        if (AuthHMAC384SHA512.ID.equals(authProtocol)) {
            return "SHA-512";
        }
        return null;
    }

    /**
     * get human readable label of privProtocol
     *
     * @return
     */
    public String getPrivProtocolLabel() {
        if (PrivAES128.ID.equals(privProtocol)) {
            return "AES-128";
        }
        if (PrivDES.ID.equals(privProtocol)) {
            return "DES";
        }
        if (Priv3DES.ID.equals(privProtocol)) {
            return "Triple DES";
        }
        if (PrivAES192.ID.equals(privProtocol)) {
            return "AES-192";
        }
        if (PrivAES192With3DESKeyExtension.ID.equals(privProtocol)) {
            return "AES-192+DES";
        }
        if (PrivAES256.ID.equals(privProtocol)) {
            return "AES-256";
        }
        return null;
    }

    public boolean isConnectionTestNeeded() {
        return isConnectionTestNeeded;
    }

    public void setConnectionTestNeeded(boolean connectionTestNeeded) {
        isConnectionTestNeeded = connectionTestNeeded;
    }

    public boolean isDummy() {
        return isDummy;
    }

    public void setDummy(boolean dummy) {
        isDummy = dummy;
    }

    public boolean isIpv6() {
        return isIpv6;
    }

    public void setIpv6(boolean ipv6) {
        isIpv6 = ipv6;
    }

    public long getLastPingTime() {
        return lastPingTime;
    }

    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    @Override
    public String toString() {
        return "DeviceConfiguration{" +
                "snmpVersion=" + snmpVersion +
                ", targetIp='" + targetIp + '\'' +
                ", targetPort=" + targetPort +
                ", networkProtocol='" + networkProtocol + '\'' +
                ", authProtocol=" + authProtocol +
                ", privProtocol=" + privProtocol +
                ", retries=" + retries +
                ", timeout=" + timeout +
                ", username='" + username + '\'' +
                ", authPassphrase='" + authPassphrase + '\'' +
                ", privacyPassphrase='" + privacyPassphrase + '\'' +
                ", isDummy=" + isDummy +
                ", isIpv6=" + isIpv6 +
                ", lastPingTime=" + lastPingTime +
                '}';
    }

    /**
     * timeout offset config
     *
     * @return
     */
    public int getAdditionalTimeoutOffset() {
        return DEFAULT_TIMEOUT_OFFSET;
    }

    /**
     * enum of supported snmp versions
     */
    public enum SNMP_VERSION {
        v1, v2c, v3
    }
}
