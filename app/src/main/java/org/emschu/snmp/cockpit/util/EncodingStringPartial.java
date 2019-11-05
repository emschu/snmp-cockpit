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

package org.emschu.snmp.cockpit.util;

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
import org.snmp4j.smi.OID;

/**
 * this class represents (all optional) values in "enc" field of device qr code.
 * with simple mapper inline classes {@link AuthProtocol}, {@link PrivProtocol} and {@link SecurityLevel}.
 */
public class EncodingStringPartial {
    private final SecurityLevel securityLevel;
    private final AuthProtocol authProtocol;
    private final PrivProtocol privProtocol;
    private final String context;
    private final String passsword;

    /**
     *
     * @param securityLevel
     * @param authProtocol
     * @param privProtocol
     * @param context
     * @param passsword
     */
    public EncodingStringPartial(SecurityLevel securityLevel, AuthProtocol authProtocol, PrivProtocol privProtocol, String context, String passsword) {
        this.securityLevel = securityLevel;
        this.authProtocol = authProtocol;
        this.privProtocol = privProtocol;
        this.context = context;
        this.passsword = passsword;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public AuthProtocol getAuthProtocol() {
        return authProtocol;
    }

    public PrivProtocol getPrivProtocol() {
        return privProtocol;
    }

    public String getContext() {
        return context;
    }

    public String getPassword() {
        return passsword;
    }

    public enum SecurityLevel {
        AUTH_PRIV(org.snmp4j.security.SecurityLevel.authPriv),
        AUTH_NO_PRIV(org.snmp4j.security.SecurityLevel.authNoPriv),
        NOAUTH_NOPRIV(org.snmp4j.security.SecurityLevel.noAuthNoPriv);

        private org.snmp4j.security.SecurityLevel snmp4jSecLevel;

        SecurityLevel(org.snmp4j.security.SecurityLevel snmp4jSecLevel) {
            this.snmp4jSecLevel = snmp4jSecLevel;
        }

        public org.snmp4j.security.SecurityLevel getSnmp4jSecLevel() {
            return snmp4jSecLevel;
        }
    }

    public enum AuthProtocol {
        SHA1(AuthSHA.ID),
        MD5(AuthMD5.ID),
        HMAC128SHA224(AuthHMAC128SHA224.ID),
        HMAC192SHA256(AuthHMAC192SHA256.ID),
        HMAC256SHA384(AuthHMAC256SHA384.ID),
        HMAC384SHA512(AuthHMAC384SHA512.ID);

        private final OID authOID;

        AuthProtocol(OID authOID) {
            this.authOID = authOID;
        }

        public OID getAuthOID() {
            return authOID;
        }
    }

    public enum PrivProtocol {
        AES128(PrivAES128.ID),
        DES(PrivDES.ID),
        AES192(PrivAES192.ID),
        AES256(PrivAES256.ID),
        DES3(Priv3DES.ID);

        private final OID privOID;

        PrivProtocol(OID privOID) {
            this.privOID = privOID;
        }

        public OID getPrivOID() {
            return privOID;
        }
    }
}
