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

package org.emschu.snmp.cockpit.query.impl.bsd;

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;
import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.6.1.4.1.30155.2.1.2
 */
public class SensorTableQuery extends AbstractSnmpTableQuery {
    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("sensorIndex", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 1}));
        columnDefinition.put("sensorDescr", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 2}));
        columnDefinition.put("sensorType", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 3}));
        columnDefinition.put("sensorDevice", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 4}));
        columnDefinition.put("sensorValue", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 5}));
        columnDefinition.put("sensorUnits", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 6}));
        columnDefinition.put("sensorStatus", new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2, 1, 7}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index) {
        return concatenateIfPossible(singleRow, new String[]{
                "sensorIndex",
                "sensorType",
                "sensorDescr"
        }, "-");
    }

    public static class SensorTableQueryRequest extends AbstractTableQueryRequest<SensorTableQuery> {

        public SensorTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public SensorTableQueryRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 4, 1, 30155, 2, 1, 2});
        }

        @Override
        public Class<SensorTableQuery> getQueryClass() {
            return SensorTableQuery.class;
        }
    }
}
