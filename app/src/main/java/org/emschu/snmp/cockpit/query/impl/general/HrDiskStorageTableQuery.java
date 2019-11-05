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
public class HrDiskStorageTableQuery extends AbstractSnmpTableQuery {

    public static final String TAG = HrDiskStorageTableQuery.class.getName();

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("hrDiskStorageAccess", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 6, 1, 1}));
        columnDefinition.put("hrDiskStorageMedia", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 6, 1, 2}));
        columnDefinition.put("hrDiskStorageRemoveble", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 6, 1, 3}));
        columnDefinition.put("hrDiskStorageCapacity", new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 6, 1, 4}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return concatenateIfPossible(singleRow, new String[]{
                "hrDiskStorageMedia",
        }, "-");
    }

    /**
     * request class
     */
    public static class HrDiskStorageTableRequest extends AbstractTableQueryRequest<HrDiskStorageTableQuery> {
        public HrDiskStorageTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 25, 3, 6});
        }

        @Override
        public Class<HrDiskStorageTableQuery> getQueryClass() {
            return HrDiskStorageTableQuery.class;
        }
    }
}
