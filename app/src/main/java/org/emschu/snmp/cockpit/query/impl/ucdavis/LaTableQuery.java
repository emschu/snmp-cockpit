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

package org.emschu.snmp.cockpit.query.impl.ucdavis;

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;
import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.3.6.1.4.1.2021.10
 */
public class LaTableQuery extends AbstractSnmpTableQuery {
    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("laIndex", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 1}));
        columnDefinition.put("laNames", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 2}));
        columnDefinition.put("laLoad", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 3}));
        columnDefinition.put("laConfig", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 4}));
        columnDefinition.put("laLoadInt", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 5}));
        columnDefinition.put("laLoadFloat", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 6}));
        columnDefinition.put("laErrorFlag", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 100}));
        columnDefinition.put("laErrMessage", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10, 1, 101}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index) {
        return concatenateIfPossible(singleRow, new String[]{
                "laIndex",
                "laNames",
        }, "-");
    }

    public static class LaTableQueryRequest extends AbstractTableQueryRequest<LaTableQuery> {

        public LaTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public LaTableQueryRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 10});
        }

        @Override
        public Class<LaTableQuery> getQueryClass() {
            return LaTableQuery.class;
        }
    }
}
