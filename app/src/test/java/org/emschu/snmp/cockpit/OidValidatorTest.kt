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

package org.emschu.snmp.cockpit

import org.emschu.snmp.cockpit.snmp.OidValidator
import org.junit.Assert
import org.junit.Test

class OidValidatorTest {

    @Test
    fun testValidOIDStrings() {
        val validOidStrings = listOf(
            "1", "1.2", "1.3.1.2.4.5", "1.10000.2000.123.123.1"
        )
        validOidStrings.forEach {
            Assert.assertTrue(OidValidator.isOidValid(it))
        }
    }

    @Test
    fun testInvalidOidStrings() {
        val invalidOidStrings = listOf(
            "",
            "       ",
            ",",
            ",,",
            "1,2,3",
            ".",
            "..",
            "...",
            " .",
            " . .",
            " . . ",
            "\uD83D\uDE00",
            "null",
            " abc ",
            " a . a ",
            "1.",
            "1.1.",
            "3.2",
            "3.2.2.1",
        )
        invalidOidStrings.forEach {
            Assert.assertFalse(OidValidator.isOidValid(it))
        }
    }
}