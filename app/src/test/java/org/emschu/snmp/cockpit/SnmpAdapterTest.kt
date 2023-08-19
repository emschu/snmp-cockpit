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
import org.emschu.snmp.cockpit.snmp.adapter.V1Adapter
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.smi.UdpAddress

@Suppress("unused")
@RunWith(MockitoJUnitRunner::class)
class SnmpAdapterTest {

    var deviceConfiguration =
        DeviceConfiguration(targetIp = "127.0.0.1", snmpVersion = DeviceConfiguration.SNMP_VERSION.v2c)
    var isV1 = true

    var snmp: Snmp = Mockito.mock(Snmp::class.java)

    var udpAddressTransportMapping: TransportMapping<*>? = Mockito.mock(TransportMapping::class.java)

    @InjectMocks
    var v1Adapter: V1Adapter = V1Adapter(
        deviceConfiguration, snmp, udpAddressTransportMapping as TransportMapping<UdpAddress>, isV1
    )

    @Test
    fun testV1Adapter() {
        Assert.assertTrue(v1Adapter.isV1)
        Assert.assertNotNull(v1Adapter.deviceConfiguration)
        Assert.assertNotNull(v1Adapter.snmp)
        Assert.assertNotNull(v1Adapter.buildTarget())
    }
}