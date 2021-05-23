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
 * this class represents a query for ip net to media table 1.3.6.1.2.1.8.5
 */
public class EgpNeighTableQuery extends AbstractSnmpTableQuery {

    public static final String TAG = EgpNeighTableQuery.class.getName();

    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("egpNeighState", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 1}));
        columnDefinition.put("egpNeighAddr", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 2}));
        columnDefinition.put("egpNeighAs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 3}));
        columnDefinition.put("egpNeighInMsgs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 4}));
        columnDefinition.put("egpNeighInErrs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 5}));
        columnDefinition.put("egpNeighOutMsgs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 6}));
        columnDefinition.put("egpNeighOutErrs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 7}));
        columnDefinition.put("egpNeighInErrMsgs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 8}));
        columnDefinition.put("egpNeighOutErrMsgs", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 9}));
        columnDefinition.put("egpNeighStateUps", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 10}));
        columnDefinition.put("egpNeighStateDowns", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 11}));
        columnDefinition.put("egpNeighIntervalHello", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 12}));
        columnDefinition.put("egpNeighIntervalPoll", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 13}));
        columnDefinition.put("egpNeighMode", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 14}));
        columnDefinition.put("egpNeighEventTrigger", new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5, 1, 15}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return concatenateIfPossible(singleRow, new String[]{
                "egpNeighAddr"
        }, "-");
    }

    /**
     * request class
     */
    public static class EgpNeighTableQueryRequest extends AbstractTableQueryRequest<EgpNeighTableQuery> {
        public EgpNeighTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public EgpNeighTableQueryRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 8, 5});
        }

        @Override
        public Class<EgpNeighTableQuery> getQueryClass() {
            return EgpNeighTableQuery.class;
        }
    }
}
