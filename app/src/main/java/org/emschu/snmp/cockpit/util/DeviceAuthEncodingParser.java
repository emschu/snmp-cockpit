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

/**
 * "enc": "accesskey",
 * "enc": "1;2;1;;accesskey",
 * "enc": ";;;;accesskey",
 * "enc": "SecurityLevel;AuthProtocol;PrivProtocol;Context;Password",
 */
public class DeviceAuthEncodingParser {

    private static final int PARTS = 5;

    private DeviceAuthEncodingParser() {}

    /**
     *
     * method to decode an enc string
     *
     * @param qrEncField
     * @return
     */
    public static EncodingStringPartial decodeString(String qrEncField) {
        EncodingStringPartial.SecurityLevel securityLevel = null;
        EncodingStringPartial.AuthProtocol authProtocol = null;
        EncodingStringPartial.PrivProtocol privProtocol = null;
        String context = null;
        String password = null;
        if (qrEncField == null || qrEncField.trim().isEmpty()) {
            // assume password was set
            securityLevel = EncodingStringPartial.SecurityLevel.NOAUTH_NOPRIV;
        } else {
            if (!qrEncField.contains(";")) {
                // use default mode and use as password
                securityLevel = EncodingStringPartial.SecurityLevel.AUTH_PRIV;
                password = qrEncField;
            } else {
                SemicolonDividedString semicolonDividedString = new SemicolonDividedString(qrEncField).invoke();
                if (semicolonDividedString.is()) {
                    return null;
                }
                securityLevel = semicolonDividedString.getSecurityLevel();
                authProtocol = semicolonDividedString.getAuthProtocol();
                privProtocol = semicolonDividedString.getPrivProtocol();
                context = semicolonDividedString.getContext();
                password = semicolonDividedString.getPassword();
            }
        }
        return new EncodingStringPartial(securityLevel, authProtocol, privProtocol, context, password);
    }

    /**
     * helper method to retrieve int
     *
     * @param part
     * @return
     */
    private static Integer getInt(String part) {
        try {
            return Integer.valueOf(part);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    /**
     * helper class which wraps functionality
     */
    private static class SemicolonDividedString {
        private boolean myResult;
        private final String qrEncField;
        private EncodingStringPartial.AuthProtocol authProtocol;
        private EncodingStringPartial.PrivProtocol privProtocol;
        private String context;
        private String password;
        private EncodingStringPartial.SecurityLevel securityLevel;

        public SemicolonDividedString(String qrEncField) {
            this.qrEncField = qrEncField;
        }

        boolean is() {
            return myResult;
        }

        public EncodingStringPartial.SecurityLevel getSecurityLevel() {
            return securityLevel;
        }

        public EncodingStringPartial.AuthProtocol getAuthProtocol() {
            return authProtocol;
        }

        public EncodingStringPartial.PrivProtocol getPrivProtocol() {
            return privProtocol;
        }

        public String getContext() {
            return context;
        }

        public String getPassword() {
            return password;
        }

        public SemicolonDividedString invoke() {
            String[] parts = qrEncField.split(";", PARTS);
            if (parts.length != PARTS) {
                myResult = true;
                return this;
            }
            // handle first
            if (parts[0].isEmpty() && !parts[3].isEmpty()) {
                securityLevel = EncodingStringPartial.SecurityLevel.AUTH_PRIV;
            } else {
                handleSecurityLevel(parts[0]);
            }
            // handle second "auth protocol"
            if (!parts[1].isEmpty()) {
                handleAuthProtocol(parts[1]);
            }
            // handle third "priv protocol"
            if (!parts[2].isEmpty()) {
                handlePrivProtocol(parts[2]);
            }
            // handle fourth context
            if (!parts[3].isEmpty()) {
                context = parts[3];
            }
            // handle fifth password
            if (!parts[4].isEmpty()) {
                password = parts[4];
            }
            myResult = false;
            return this;
        }

        public void handlePrivProtocol(String part) {
            EncodingStringPartial.PrivProtocol[] privProtocols = EncodingStringPartial.PrivProtocol.values();
            Integer privP = getInt(part);
            if (privP != null) {
                if (privProtocols.length > privP) {
                    privProtocol = privProtocols[privP];
                }
            }
        }

        public void handleAuthProtocol(String part) {
            EncodingStringPartial.AuthProtocol[] authProtocols = EncodingStringPartial.AuthProtocol.values();
            Integer authP = getInt(part);
            if (authP != null) {
                if (authProtocols.length > authP) {
                    authProtocol = authProtocols[authP];
                }
            }
        }

        public void handleSecurityLevel(String part) {
            Integer secLevel = getInt(part);
            if (secLevel == null) {
                securityLevel = EncodingStringPartial.SecurityLevel.AUTH_PRIV;
            } else {
                EncodingStringPartial.SecurityLevel[] secLevels = EncodingStringPartial.SecurityLevel.values();
                if (secLevels.length > secLevel) {
                    securityLevel = secLevels[secLevel];
                } else {
                    securityLevel = EncodingStringPartial.SecurityLevel.AUTH_PRIV;
                }
            }
        }
    }
}
