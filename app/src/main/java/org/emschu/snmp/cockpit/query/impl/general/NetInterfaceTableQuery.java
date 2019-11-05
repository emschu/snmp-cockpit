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
import java.util.Set;

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * this class represents a query for network 1.3.6.1.2.1.2.2.1.*
 */
public class NetInterfaceTableQuery extends AbstractSnmpTableQuery {

    public static final String TAG = NetInterfaceTableQuery.class.getName();

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("ifIndex", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 1}));
        columnDefinition.put("ifDescr", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 2}));
        columnDefinition.put("ifType", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 3}));
        columnDefinition.put("ifMtu", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 4}));
        columnDefinition.put("ifSpeed", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 5}));
        columnDefinition.put("ifPhysAddress", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 6}));
        columnDefinition.put("ifAdminStatus", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 7}));
        columnDefinition.put("ifOperStatus", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 8}));
        columnDefinition.put("ifLastChange", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 9}));
        columnDefinition.put("ifInOctets", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 10}));
        columnDefinition.put("ifInUcastPkts", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 11}));
        columnDefinition.put("ifInNUcastPkts", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 12}));
        columnDefinition.put("ifInDiscards", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 13}));
        columnDefinition.put("ifInErrors", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 14}));
        columnDefinition.put("ifInUnknownProtos", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 15}));
        columnDefinition.put("ifOutOctets", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 16}));
        columnDefinition.put("ifOutUcastPkts", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 17}));
        columnDefinition.put("ifOutNUcastPkts", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 18}));
        columnDefinition.put("ifOutDiscards", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 19}));
        columnDefinition.put("ifOutErrors", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 20}));
        columnDefinition.put("ifOutQLen", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 21}));
        columnDefinition.put("ifSpecific", new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2, 1, 22}));
        return columnDefinition;
    }

    @Override
    public String[] getHeaderColumns() {
        Set<String> strings = getColumnDefinition().keySet();
        return strings.toArray(new String[]{});
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return concatenateIfPossible(singleRow, new String[]{
                "ifDescr",
                "ifIndex",
                "ifPhysAddress",
        }, "-");
    }

    /**
     * request class
     */
    public static class NetInterfaceTableRequest extends AbstractTableQueryRequest<NetInterfaceTableQuery> {
        public NetInterfaceTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 2, 2});
        }

        @Override
        public Class<NetInterfaceTableQuery> getQueryClass() {
            return NetInterfaceTableQuery.class;
        }
    }
}
