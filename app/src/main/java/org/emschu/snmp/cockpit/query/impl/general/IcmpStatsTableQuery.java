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
 * this class represents a query for ip route table 1.3.6.1.2.1.4.31.3
 */
public class IcmpStatsTableQuery extends AbstractSnmpTableQuery {

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();

        columnDefinition.put("icmpStatsIPVersion", new OID(new int[]{1, 3, 6, 1, 2, 1, 5, 29, 1, 1}));
        columnDefinition.put("icmpStatsInMsgs", new OID(new int[]{1, 3, 6, 1, 2, 1, 5, 29, 1, 2}));
        columnDefinition.put("icmpStatsInErrors", new OID(new int[]{1, 3, 6, 1, 2, 1, 5, 29, 1, 3}));
        columnDefinition.put("icmpStatsOutMsgs", new OID(new int[]{1, 3, 6, 1, 2, 1, 5, 29, 1, 4}));
        columnDefinition.put("icmpStatsOutErrors", new OID(new int[]{1, 3, 6, 1, 2, 1, 5, 29, 1, 5}));

        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return String.valueOf(rowIndex) + concatenateIfPossible(singleRow, new String[]{
                "icmpStatsIPVersion",
        }, null);
    }

    /**
     * request class
     */
    public static class IcmpStatsTableRequest extends AbstractTableQueryRequest<IcmpStatsTableQuery> {
        public IcmpStatsTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 5, 29});
        }

        @Override
        public Class<IcmpStatsTableQuery> getQueryClass() {
            return IcmpStatsTableQuery.class;
        }
    }
}
