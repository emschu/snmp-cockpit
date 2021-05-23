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

package org.emschu.snmp.cockpit.query.impl.general;

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;
import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

/**
 * this class represents a query for ip address table 1.3.6.1.2.1.4.20
 */
public class IpAddressTableQuery extends AbstractSnmpTableQuery {
    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("ipAdEntAddr", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 20, 1, 1}));
        columnDefinition.put("ipAdEntIfIndex", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 20, 1, 2}));
        columnDefinition.put("ipAdEntNetMask", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 20, 1, 3}));
        columnDefinition.put("ipAdEntBcastAddr", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 20, 1, 4}));
        columnDefinition.put("ipAdEntReasmMaxSize", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 20, 1, 5}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index) {
        return concatenateIfPossible(singleRow, new String[]{
                "ipAdEntIfIndex",
                "ipAdEntAddr",
        }, "-");
    }

    /**
     * request class
     */
    public static class IpAddrTableRequest extends AbstractTableQueryRequest<IpAddressTableQuery> {
        public IpAddrTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public IpAddrTableRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 20});
        }

        @Override
        public Class<IpAddressTableQuery> getQueryClass() {
            return IpAddressTableQuery.class;
        }
    }
}
