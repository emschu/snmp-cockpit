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

import org.emschu.snmp.cockpit.ui.components.FormValidator
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FormValidatorTest {
    @Test
    fun testPortIntegerAndNumericValidator() {
        val validPortNumbers = arrayListOf<String>("1", "2", "3", "161", "1024", "65535")
        val inValidPortNumbers = arrayListOf<String>("0", "65536", "-1", "-100")

        validPortNumbers.forEach {
            Assert.assertTrue(FormValidator.PORT_INTEGER.isValid(it))
            Assert.assertTrue(FormValidator.NUMERIC_POSITIVE.isValid(it))
        }
        inValidPortNumbers.forEach {
            Assert.assertFalse(FormValidator.PORT_INTEGER.isValid(it))
        }
        Assert.assertTrue(FormValidator.NUMERIC_POSITIVE.isValid("0"))
        Assert.assertTrue(FormValidator.NUMERIC_POSITIVE.isValid("65537"))
    }

    @Test
    fun testIpv4Validator() {
        val validIpv4Addresses = arrayListOf<String>("127.0.0.1", "10.10.1.1", "192.168.178.1")
        val invalidIpv4Addresses =
            arrayListOf<String>("", "localhost", "127001", "256.256.256.256", "127.0.0.1.", ".127.0.0.1")

        validIpv4Addresses.forEach {
            Assert.assertTrue(FormValidator.IP_ADDRESS_V4.isValid(it))
            Assert.assertFalse(FormValidator.IP_ADDRESS_V6.isValid(it))
        }
        invalidIpv4Addresses.forEach {
            Assert.assertFalse(FormValidator.IP_ADDRESS_V4.isValid(it))
            Assert.assertFalse(FormValidator.IP_ADDRESS_V6.isValid(it))
        }
    }

    @Test
    fun testIpv6Validator() {
        val validIpv6Addresses =
            arrayListOf<String>("2001:db8:3333:4444:5555:6666:7777:8888", "fe80::42:abcd:abcd:abcd")
        val invalidIpv6Addresses = arrayListOf<String>("", "..1", "1234123")

        validIpv6Addresses.forEach {
            Assert.assertTrue(FormValidator.IP_ADDRESS_V6.isValid(it))
            Assert.assertFalse(FormValidator.IP_ADDRESS_V4.isValid(it))
        }
        invalidIpv6Addresses.forEach {
            Assert.assertFalse(FormValidator.IP_ADDRESS_V6.isValid(it))
            Assert.assertFalse(FormValidator.IP_ADDRESS_V4.isValid(it))
        }
    }

    @Test
    fun testNotEmptyValidator() {
        val notEmpty = arrayListOf<String>("a", "1", "   a")
        val empty = arrayListOf<String>("", " ", "   ", "\t", "           ")

        empty.forEach {
            Assert.assertFalse(FormValidator.NOT_EMPTY.isValid(it))
            Assert.assertFalse(FormValidator.LONGER_THAN_8.isValid(it))
        }
        notEmpty.forEach {
            Assert.assertTrue(FormValidator.NOT_EMPTY.isValid(it))
            Assert.assertFalse(FormValidator.LONGER_THAN_8.isValid(it))
        }
    }
}