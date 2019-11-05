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

import java.util.regex.Pattern;

/**
 * class which provides several static format conversion utility methods
 *
 * TODO test this class
 */
public class Converter {
    private static final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");

    /**
     * Encloses the incoming string inside double quotes, if it isn't already quoted.
     *
     * @param s the input string
     * @return a quoted string, of the form "input".  If the input string is null, it returns null
     * as well.
     */
    public static String convertToQuotedString(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        // If already quoted, return as-is
        if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s;
        }
        return '\"' + s + '\"';
    }

    /**
     * @param value          input to check
     * @param allowedLengths allowed lengths, if any
     * @return true if value is a non-null, non-empty string of hex digits, and if allowed lengths are given, has
     * an allowed length
     */
    public static boolean isHexOfLength(CharSequence value, int... allowedLengths) {
        if (value == null || !HEX_DIGITS.matcher(value).matches()) {
            return false;
        }
        if (allowedLengths.length == 0) {
            return true;
        }
        for (int length : allowedLengths) {
            if (value.length() == length) {
                return true;
            }
        }
        return false;
    }

    /**
     * integer to ip conversion
     *
     * @param integer
     * @return
     */
    public static String intToIp(int integer) {
        return (integer & 0xFF) + "." + ((integer >> 8) & 0xFF) + "."
                + ((integer >> 16) & 0xFF) + "." + ((integer >> 24) & 0xFF);
    }

    /**
     * quote non hex strings
     *
     * @param value
     * @param allowedLengths
     * @return
     */
    public static String quoteNonHex(String value, int... allowedLengths) {
        return Converter.isHexOfLength(value, allowedLengths) ? value : Converter.convertToQuotedString(value);
    }
}
