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

import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.ui.components.generateQrCodeContentForDevice
import org.emschu.snmp.cockpit.ui.components.getDeviceQrCode
import org.junit.Assert
import org.junit.Test

class QrCodeContentTest {

    @Test
    fun testParsingOfDeviceQrCode() {
        val deviceQrCode = getDeviceQrCode(
            """
            {"user": "user", "pw": "", "enc": "", "naddr": { "IPv4": "", "IPv6" : "" }}
        """.trimIndent()
        )
        Assert.assertNotNull(deviceQrCode)
        Assert.assertEquals("user", deviceQrCode!!.user)
        Assert.assertEquals("", deviceQrCode.pw)
        Assert.assertEquals("", deviceQrCode.enc)
        Assert.assertNotNull(deviceQrCode.naddr)
        Assert.assertEquals("", deviceQrCode.naddr!!.iPv4)
        Assert.assertEquals("", deviceQrCode.naddr!!.iPv6)
    }

    @Test
    fun testBasicSnmpv1CodeGeneration() {
        val v1v2Versions = arrayOf(DeviceConfiguration.SNMP_VERSION.v1, DeviceConfiguration.SNMP_VERSION.v2c)
        val testIpv4 = "127.0.0.1"
        val testIpv6 = "::2"
        val testPort = 161
        val testCommunity = "public"

        v1v2Versions.forEach { snmpVersion ->
            val deviceConfig = DeviceConfiguration(
                snmpVersion = snmpVersion,
                targetIp = testIpv4,
                targetPort = testPort,
                username = testCommunity
            )
            val qrCodeContent = generateQrCodeContentForDevice(deviceConfig)
            Assert.assertEquals(
                qrCodeContent,
                "{\"user\": \"%s\", \"pw\": \"\", \"enc\": \"\", \"naddr\": {\"IPv4\": \"%s\", \"IPv6\": \"\"}}".format(
                    testCommunity,
                    testIpv4
                )
            )

            val deviceConfigIpv6 = DeviceConfiguration(
                snmpVersion,
                targetIp = testIpv6,
                targetPort = testPort,
                username = testCommunity,
                isIpv6 = true
            )
            val qrCodeContentv6 = generateQrCodeContentForDevice(deviceConfigIpv6)
            Assert.assertEquals(
                qrCodeContentv6,
                "{\"user\": \"%s\", \"pw\": \"\", \"enc\": \"\", \"naddr\": {\"IPv4\": \"\", \"IPv6\": \"%s\"}}".format(
                    testCommunity,
                    testIpv6
                )
            )
        }
    }

    @Test
    fun testBasicSnmpv3CodeGeneration() {
        val testIpv4 = "127.0.0.1"
        val testIpv6 = "::2"
        val testPort = 161
        val testCommunity = "public"

        val deviceConfig = DeviceConfiguration(
            snmpVersion = DeviceConfiguration.SNMP_VERSION.v3,
            targetIp = testIpv4,
            targetPort = testPort,
            username = testCommunity
        )
        val qrCodeContent = generateQrCodeContentForDevice(deviceConfig)
        Assert.assertEquals(
            qrCodeContent,
            "{\"user\": \"%s\", \"pw\": \"\", \"enc\": \"\", \"naddr\": {\"IPv4\": \"%s\", \"IPv6\": \"\"}}".format(
                testCommunity,
                testIpv4
            )
        )

        val deviceConfigIpv6 = DeviceConfiguration(
            DeviceConfiguration.SNMP_VERSION.v3,
            targetIp = testIpv6,
            targetPort = testPort,
            username = testCommunity,
            isIpv6 = true,
        )
        val qrCodeContentv6 = generateQrCodeContentForDevice(deviceConfigIpv6)
        Assert.assertEquals(
            qrCodeContentv6,
            "{\"user\": \"%s\", \"pw\": \"\", \"enc\": \"\", \"naddr\": {\"IPv4\": \"\", \"IPv6\": \"%s\"}}".format(
                testCommunity,
                testIpv6
            )
        )
    }

    // TODO test all snmp v3 possibilities, fields + enc and priv support
}