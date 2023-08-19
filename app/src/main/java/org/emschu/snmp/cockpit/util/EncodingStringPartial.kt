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
package org.emschu.snmp.cockpit.util

import org.emschu.snmp.cockpit.util.EncodingStringPartial.AuthProtocol
import org.emschu.snmp.cockpit.util.EncodingStringPartial.PrivProtocol
import org.emschu.snmp.cockpit.util.EncodingStringPartial.SecurityLevel
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
import org.snmp4j.smi.OID

/**
 * this class represents (all optional) values in "enc" field of device qr code.
 * with simple mapper inline classes [AuthProtocol], [PrivProtocol] and [SecurityLevel].
 */
class EncodingStringPartial(
    val securityLevel: SecurityLevel?,
    val authProtocol: AuthProtocol?,
    val privProtocol: PrivProtocol?,
    val context: String?,
    val password: String?,
) {

    enum class SecurityLevel(val snmp4jSecLevel: org.snmp4j.security.SecurityLevel) {
        AUTH_PRIV(org.snmp4j.security.SecurityLevel.authPriv), AUTH_NO_PRIV(
            org.snmp4j.security.SecurityLevel.authNoPriv
        ),
        NOAUTH_NOPRIV(
            org.snmp4j.security.SecurityLevel.noAuthNoPriv
        );
    }

    enum class AuthProtocol(val authOID: OID) {
        SHA1(AuthSHA.ID), MD5(AuthMD5.ID), HMAC128SHA224(AuthHMAC128SHA224.ID), HMAC192SHA256(
            AuthHMAC192SHA256.ID
        ),
        HMAC256SHA384(
            AuthHMAC256SHA384.ID
        ),
        HMAC384SHA512(AuthHMAC384SHA512.ID);
    }

    enum class PrivProtocol(val privOID: OID) {
        AES128(PrivAES128.ID), DES(PrivDES.ID), AES192(PrivAES192.ID), AES256(PrivAES256.ID), DES3(Priv3DES.ID);
    }
}