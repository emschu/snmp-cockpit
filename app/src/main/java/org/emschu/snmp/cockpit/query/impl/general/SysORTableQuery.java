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

import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * table query for mibs 1.3.6.1.2.1.1.9 - sysORTable
 */
public class SysORTableQuery extends AbstractSnmpTableQuery {
    @Override
    public Map<String, OID> getColumnDefinition() {
        HashMap<String, OID> columns = new HashMap<>();
        columns.put("sysORIndex", new OID(new int[]{1, 3, 6, 1, 2, 1, 1, 9, 1, 1}));
        columns.put("sysORID", new OID(new int[]{1, 3, 6, 1, 2, 1, 1, 9, 1, 2}));
        columns.put("sysORDescr", new OID(new int[]{1, 3, 6, 1, 2, 1, 1, 9, 1, 3}));
        columns.put("sysORUpTime", new OID(new int[]{1, 3, 6, 1, 2, 1, 1, 9, 1, 4}));
        return columns;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index) {
        return concatenateIfPossible(singleRow, new String[]{"sysORDescr"}, "-");
    }

    public static class SysORTableQueryRequest extends AbstractQueryRequest<SysORTableQuery> {

        public SysORTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public boolean isSingleRequest() {
            return false;
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 1, 9});
        }

        @Override
        public Class<SysORTableQuery> getQueryClass() {
            return SysORTableQuery.class;
        }
    }
}
