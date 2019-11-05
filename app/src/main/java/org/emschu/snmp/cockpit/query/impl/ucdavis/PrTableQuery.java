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

import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * 1.3.6.1.4.1.2021.2
 *
 * atm not used
 */
public class PrTableQuery extends AbstractSnmpTableQuery {
    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("prIndex", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 1}));
        columnDefinition.put("prNames", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 2}));
        columnDefinition.put("prMin", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 3}));
        columnDefinition.put("prMax", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 4}));
        columnDefinition.put("prCount", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 5}));
        columnDefinition.put("prErrorFlag", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 100}));
        columnDefinition.put("prErrMessage", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 101}));
        columnDefinition.put("prErrFix", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 102}));
        columnDefinition.put("prErrFixCmd", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2, 1, 103}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index) {
        return concatenateIfPossible(singleRow, new String[]{
                "prIndex",
                "prNames",
        }, "-");
    }

    public static class PrTableQueryRequest extends AbstractTableQueryRequest<PrTableQuery> {

        public PrTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 2});
        }

        @Override
        public Class<PrTableQuery> getQueryClass() {
            return PrTableQuery.class;
        }
    }
}
