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

package org.emschu.snmp.cockpit;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import org.emschu.snmp.cockpit.snmp.json.DeviceQrCode;

public class DeviceQrCodeJsonTokenTest {

    // this should be the input after specs
    private static final String correctQrCodeString1 = "{\n" +
            "    \"user\": \"batmanuser\",\n" +
            "    \"pw\": \"password123\",\n" +
            "    \"enc\": \"batmankey3\",\n" +
            "    \"naddr\": {\n" +
            "        \"IPv4\": \"192.168.161.1:162\",\n" +
            "        \"IPv6\": \"TODO\"\n" +
            "    }\n" +
            "}";

    // .. and flattened
    private static final String correctQrCodeString2 = "{\"user\": \"batmanuser\",\"pw\": \"password123\",\"enc\": \"batmankey3\",\"naddr\": {\"IPv4\": \"192.168.161.1:162\",\"IPv6\": \"TODO\"}}";

    // incorrect json
    private static final String incorrectQrCodeString1 = "{\"user\": \"batmanuser\"\"pw\": \"password123\",\"enc\": \"batmankey3\",\"naddr\": {\"IPv4\": \"192.168.161.1:162\",\"IPv6\": \"TODO\"}}";

    @Test
    public void testCorrectQrCodeDeserialization() {
        ObjectMapper om = new ObjectMapper();
        try {
            DeviceQrCode code = om.readValue(correctQrCodeString1, DeviceQrCode.class);
            Assert.assertNotNull(code);
            checkQrCode(code);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            DeviceQrCode code2 = om.readValue(correctQrCodeString2, DeviceQrCode.class);

            Assert.assertNotNull(code2);
            checkQrCode(code2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IOException.class)
    public void testInvalidJsonDoesntWork() throws IOException {
        ObjectMapper om = new ObjectMapper();
        DeviceQrCode code2 = null;
        code2 = om.readValue(incorrectQrCodeString1, DeviceQrCode.class);
        Assert.assertNull(code2);

    }

    private void checkQrCode(DeviceQrCode code) {
        Assert.assertEquals("batmanuser", code.getUser());
        Assert.assertEquals("password123", code.getPw());
        Assert.assertEquals("batmankey3", code.getEnc());
        Assert.assertEquals("192.168.161.1:162", code.getNaddr().getIPv4());
        Assert.assertEquals("TODO", code.getNaddr().getIPv6());
        Assert.assertEquals(162, code.getPortv4());
    }
}
