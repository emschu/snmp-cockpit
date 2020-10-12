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
 * this class represents a query for 1.3.6.1.2.1.25.3.6
 */
public class HrPartitionTableQuery extends AbstractSnmpTableQuery {

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("hrPartitionIndex", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 7, 1, 1}));
        columnDefinition.put("hrPartitionLabel", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 7, 1, 2}));
        columnDefinition.put("hrPartitionID", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 7, 1, 3}));
        columnDefinition.put("hrPartitionSize", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 7, 1, 4}));
        columnDefinition.put("hrPartitionFSIndex", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 7, 1, 5}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return concatenateIfPossible(singleRow, new String[]{
                "hrPartitionIndex",
                "hrPartitionID",
                "hrPartitionLabel"
        }, "-");
    }

    /**
     * request class
     */
    public static class HrPartitionTableRequest extends AbstractTableQueryRequest<HrPartitionTableQuery> {
        public HrPartitionTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 7});
        }

        @Override
        public Class<HrPartitionTableQuery> getQueryClass() {
            return HrPartitionTableQuery.class;
        }
    }
}
