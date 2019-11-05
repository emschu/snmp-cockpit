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

import org.junit.Assert;
import org.junit.Test;

import org.emschu.snmp.cockpit.util.DeviceAuthEncodingParser;
import org.emschu.snmp.cockpit.util.EncodingStringPartial;

/**
 * unit test for v3 encoding string in qr code
 */
public class AuthEncodingParserTest {

    private String[] wrongCodes = new String[]{
        ";;;testpw",
        ";;testpw",
        ";testpw"
    };

    @Test
    public void testCorrectExamles() {
        EncodingStringPartial encodingStringPartial1 = DeviceAuthEncodingParser.decodeString(";;;;testpw");
        Assert.assertNotNull(encodingStringPartial1);
        Assert.assertNull(encodingStringPartial1.getAuthProtocol());
        Assert.assertNull(encodingStringPartial1.getContext());
        Assert.assertNull(encodingStringPartial1.getPrivProtocol());
        Assert.assertEquals(encodingStringPartial1.getPassword(), "testpw");
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.AUTH_PRIV, encodingStringPartial1.getSecurityLevel());

        EncodingStringPartial encodingStringPartial2 = DeviceAuthEncodingParser.decodeString("0;0;0;context;testpw");
        Assert.assertNotNull(encodingStringPartial2);
        Assert.assertEquals(EncodingStringPartial.AuthProtocol.SHA1, encodingStringPartial2.getAuthProtocol());
        Assert.assertEquals(EncodingStringPartial.PrivProtocol.AES128, encodingStringPartial2.getPrivProtocol());
        Assert.assertEquals("context", encodingStringPartial2.getContext());
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.AUTH_PRIV, encodingStringPartial2.getSecurityLevel());

        EncodingStringPartial encodingStringPartial3 = DeviceAuthEncodingParser.decodeString("1;1;1;context;testpw");
        Assert.assertNotNull(encodingStringPartial3);
        Assert.assertEquals(EncodingStringPartial.AuthProtocol.MD5, encodingStringPartial3.getAuthProtocol());
        Assert.assertEquals(EncodingStringPartial.PrivProtocol.DES, encodingStringPartial3.getPrivProtocol());
        Assert.assertEquals("context", encodingStringPartial3.getContext());
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.AUTH_NO_PRIV, encodingStringPartial3.getSecurityLevel());

        EncodingStringPartial encodingStringPartial4 = DeviceAuthEncodingParser.decodeString("2;1;1;context;testpw");
        Assert.assertNotNull(encodingStringPartial4);
        Assert.assertEquals(EncodingStringPartial.AuthProtocol.MD5, encodingStringPartial4.getAuthProtocol());
        Assert.assertEquals(EncodingStringPartial.PrivProtocol.DES, encodingStringPartial4.getPrivProtocol());
        Assert.assertEquals("context", encodingStringPartial4.getContext());
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.NOAUTH_NOPRIV, encodingStringPartial4.getSecurityLevel());

        EncodingStringPartial encodingStringPartial5 = DeviceAuthEncodingParser.decodeString("testpw");
        Assert.assertNotNull(encodingStringPartial5);
        Assert.assertNull(encodingStringPartial5.getAuthProtocol());
        Assert.assertNull(encodingStringPartial5.getContext());
        Assert.assertNull(encodingStringPartial5.getPrivProtocol());
        Assert.assertEquals(encodingStringPartial5.getPassword(), "testpw");
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.AUTH_PRIV, encodingStringPartial5.getSecurityLevel());

        EncodingStringPartial encodingStringPartial6 = DeviceAuthEncodingParser.decodeString("");
        Assert.assertNotNull(encodingStringPartial6);
        Assert.assertNull(encodingStringPartial6.getAuthProtocol());
        Assert.assertNull(encodingStringPartial6.getContext());
        Assert.assertNull(encodingStringPartial6.getPrivProtocol());
        Assert.assertNull(encodingStringPartial6.getPassword());
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.NOAUTH_NOPRIV, encodingStringPartial6.getSecurityLevel());
    }

    @Test
    public void testWrongExamples() {
        // empty password, missing semicolon
        for (String wrongFormattedEnc : wrongCodes) {
            EncodingStringPartial encodingStringPartial = DeviceAuthEncodingParser.decodeString(wrongFormattedEnc);
            Assert.assertNull(encodingStringPartial);
        }
    }

    @Test
    public void testNumberOverflowsOfFields() {
        EncodingStringPartial encodingStringPartial1 = DeviceAuthEncodingParser.decodeString("9;10;10;;testpw");
        Assert.assertNotNull(encodingStringPartial1);
        Assert.assertNull(encodingStringPartial1.getAuthProtocol());
        Assert.assertNull(encodingStringPartial1.getPrivProtocol());
        Assert.assertNull(encodingStringPartial1.getContext());
        Assert.assertEquals("testpw", encodingStringPartial1.getPassword());
        Assert.assertEquals(EncodingStringPartial.SecurityLevel.AUTH_PRIV, encodingStringPartial1.getSecurityLevel());
    }
}
