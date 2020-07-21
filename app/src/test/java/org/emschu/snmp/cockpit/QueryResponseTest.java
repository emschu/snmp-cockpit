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
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.VariableBinding;

import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

public class QueryResponseTest {

    @Test
    public void testQueryResponseModel() {
        QueryResponse response = new QueryResponse(SnmpConstants.sysLocation.toDottedString(), new VariableBinding());
        Assert.assertNotNull(response);
        Assert.assertEquals(SnmpConstants.sysLocation.toDottedString(), response.getOid());
        Assert.assertNotNull(response.getVariableBinding());
    }
}
