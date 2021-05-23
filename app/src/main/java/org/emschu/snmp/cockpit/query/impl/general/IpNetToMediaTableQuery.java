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
 * this class represents a query for ip net to media table 1.3.6.1.2.1.4.22
 */
public class IpNetToMediaTableQuery extends AbstractSnmpTableQuery {

    public static final String TAG = IpNetToMediaTableQuery.class.getName();

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("ipNetToMediaIfIndex", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 22, 1, 1}));
        columnDefinition.put("ipNetToMediaPhysAddress", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 22, 1, 2}));
        columnDefinition.put("ipNetToMediaNetAddress", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 22, 1, 3}));
        columnDefinition.put("ipNetToMediaType", new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 22, 1, 4}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return concatenateIfPossible(singleRow, new String[]{
                "ipNetToMediaNetAddress",
                "ipNetToMediaPhysAddress",
        }, "-");
    }

    /**
     * request class
     */
    public static class IpNetToMediaTableRequest extends AbstractTableQueryRequest<IpNetToMediaTableQuery> {
        public IpNetToMediaTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public IpNetToMediaTableRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 22});
        }

        @Override
        public Class<IpNetToMediaTableQuery> getQueryClass() {
            return IpNetToMediaTableQuery.class;
        }
    }
}
