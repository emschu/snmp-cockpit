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

import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * this class represents a query for ip net to media table 1.3.6.1.2.1.7.5
 */
public class UdpTableQuery extends AbstractSnmpTableQuery {

    public static final String TAG = UdpTableQuery.class.getName();

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("udpLocalAddress", new OID(new int[]{1, 3, 6, 1, 2, 1, 7, 5, 1, 1}));
        columnDefinition.put("udpLocalPort", new OID(new int[]{1, 3, 6, 1, 2, 1, 7, 5, 1, 2}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return concatenateIfPossible(singleRow, new String[]{
                "udpLocalAddress",
                "udpLocalPort",
        }, "|");
    }

    /**
     * request class
     */
    public static class UdpTableQueryRequest extends AbstractTableQueryRequest<UdpTableQuery> {
        public UdpTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 7, 5});
        }

        @Override
        public Class<UdpTableQuery> getQueryClass() {
            return UdpTableQuery.class;
        }
    }
}
