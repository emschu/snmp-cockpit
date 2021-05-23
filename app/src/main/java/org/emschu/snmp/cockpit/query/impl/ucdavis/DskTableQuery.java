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
 * 1.3.6.1.4.1.30155.2.1.2
 */
public class DskTableQuery extends AbstractSnmpTableQuery {
    @Override
    public Map<String, OID> getColumnDefinition() {
        Map<String, OID> columnDefinition = new HashMap<>();
        columnDefinition.put("dskIndex", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 1}));
        columnDefinition.put("dskPath", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 2}));
        columnDefinition.put("dskDevice", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 3}));
        columnDefinition.put("dskMinimum", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 4}));
        columnDefinition.put("dskMinPercent", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 5}));
        columnDefinition.put("dskTotal", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 6}));
        columnDefinition.put("dskAvail", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 7}));
        columnDefinition.put("dskUsed", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 8}));
        columnDefinition.put("dskPercent", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 9}));
        columnDefinition.put("dskPercentNode", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 10}));
        columnDefinition.put("dskTotalLow", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 11}));
        columnDefinition.put("dskTotalHigh", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 12}));
        columnDefinition.put("dskAvailLow", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 13}));
        columnDefinition.put("dskAvailHigh", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 14}));
        columnDefinition.put("dskUsedLow", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 15}));
        columnDefinition.put("dskUsedHigh", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 16}));
        columnDefinition.put("dskErrorFlag", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 17}));
        columnDefinition.put("dskErrorMsg", new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9, 1, 18}));
        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index) {
        return concatenateIfPossible(singleRow, new String[]{
                "dskIndex",
                "dskDevice",
        }, "-");
    }

    public static class DskTableQueryRequest extends AbstractTableQueryRequest<DskTableQuery> {

        public DskTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public DskTableQueryRequest(DeviceConfiguration deviceConfiguration, int hw_info_task_view_label_table_dsktable) {
            super(deviceConfiguration, hw_info_task_view_label_table_dsktable);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 9});
        }

        @Override
        public Class<DskTableQuery> getQueryClass() {
            return DskTableQuery.class;
        }
    }
}
