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

/**
 * TODO test!
 *
 * "enc": "accesskey",
 * "enc": "1;2;1;;accesskey",
 * "enc": ";;;;accesskey",
 * "enc": "SecurityLevel;AuthProtocol;PrivProtocol;Context;Password",
 */
object DeviceAuthEncodingParser {
    private const val componentCount = 5

    /**
     *
     * method to decode an enc string
     *
     * @param qrEncField
     * @return
     */
    @JvmStatic
    fun decodeString(qrEncField: String?): EncodingStringPartial {
        val securityLevel: EncodingStringPartial.SecurityLevel?
        var authProtocol: AuthProtocol? = null
        var privProtocol: PrivProtocol? = null
        var context: String? = null
        var password: String? = null
        if (qrEncField == null || qrEncField.trim()
                .isBlank()
        ) {
            // no data
            securityLevel = EncodingStringPartial.SecurityLevel.NOAUTH_NOPRIV
        } else {
            if (!qrEncField.contains(";")) {
                // use default mode and use as password
                securityLevel = EncodingStringPartial.SecurityLevel.AUTH_PRIV
                password = qrEncField
            } else {
                val semicolonDividedString = SemicolonDividedString(qrEncField).invoke()
                if (semicolonDividedString.isError) {
                    return EncodingStringPartial(null, null, null, null, null)
                }
                securityLevel = semicolonDividedString.securityLevel
                authProtocol = semicolonDividedString.authProtocol
                privProtocol = semicolonDividedString.privProtocol
                context = semicolonDividedString.context
                password = semicolonDividedString.password
            }
        }
        return EncodingStringPartial(securityLevel, authProtocol, privProtocol, context, password)
    }

    /**
     * helper class which wraps functionality
     */
    class SemicolonDividedString(private val qrEncField: String) {
        var isError = false
            private set
        var authProtocol: AuthProtocol? = null
            private set
        var privProtocol: PrivProtocol? = null
            private set
        var context: String? = null
            private set
        var password: String? = null
            private set
        var securityLevel: EncodingStringPartial.SecurityLevel? = null
            private set

        operator fun invoke(): SemicolonDividedString {
            val parts: Array<String> = qrEncField.split(Regex(";"), componentCount.coerceAtLeast(0))
                .toTypedArray()
            if (parts.size != componentCount) {
                isError = true
                return this
            }
            // handle first
            if (parts[0].isBlank() && parts[3].isNotBlank()) {
                securityLevel = EncodingStringPartial.SecurityLevel.AUTH_PRIV
            } else {
                handleSecurityLevel(parts[0])
            }
            // handle second "auth protocol"
            if (parts[1].isNotBlank()) {
                handleAuthProtocol(parts[1])
            }
            // handle third "priv protocol"
            if (parts[2].isNotBlank()) {
                handlePrivProtocol(parts[2])
            }
            // handle fourth context
            if (parts[3].isNotBlank()) {
                context = parts[3]
            }
            // handle fifth password
            if (parts[4].isNotBlank()) {
                password = parts[4]
            }
            isError = false
            return this
        }

        private fun handlePrivProtocol(part: String) {
            val privProtocols = PrivProtocol.values()
            val privP = part.toInt()
            if (privProtocols.size > privP) {
                privProtocol = privProtocols[privP]
            }
        }

        private fun handleAuthProtocol(part: String) {
            val authProtocols = AuthProtocol.values()
            val authP = part.toInt()
            if (authProtocols.size > authP) {
                authProtocol = authProtocols[authP]
            }
        }

        private fun handleSecurityLevel(part: String) {
            val secLevel = part.toInt()
            val secLevels = EncodingStringPartial.SecurityLevel.values()
            securityLevel = if (secLevels.size > secLevel) {
                secLevels[secLevel]
            } else {
                EncodingStringPartial.SecurityLevel.AUTH_PRIV
            }
        }
    }
}