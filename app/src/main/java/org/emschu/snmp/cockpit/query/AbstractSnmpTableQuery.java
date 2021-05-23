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

package org.emschu.snmp.cockpit.query;

import android.util.Log;

import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * abstract class for table query with common methods
 */
public abstract class AbstractSnmpTableQuery implements TableQuery {

    public static final String TAG = AbstractSnmpTableQuery.class.getName();
    private final Map<String, Map<String, QueryResponse>> rowList = new HashMap<>();
    private int unknownOIDCounter = -1;

    @Override
    public void processResult(List<QueryResponse> results) {
        OIDCatalog catalog = OIDCatalog.getInstance(null);
        rowList.clear();
        for (QueryResponse qr : results) {
            // strip last oid node - its the counter number
            String rowIndex = qr.getOid().substring(qr.getOid().lastIndexOf('.') + 1);
            Map<String, QueryResponse> currentRow;
            if (!rowList.containsKey(rowIndex)) {
                // lazy init rows
                currentRow = new HashMap<>();
                rowList.put(rowIndex, currentRow);
            } else {
                currentRow = rowList.get(rowIndex);
            }
            if (currentRow == null) {
                continue;
            }
            String key;
            try {
                key = catalog.getAsnByOidStripLast(qr.getOid());
            } catch (OIDNotInCatalogException e) {
                Log.d(TAG, "OID " + qr.getOid() + " not associable with asn in catalog");
                key = "unknown-" + unknownOIDCounter++;
            }
            currentRow.put(key, qr);
        }
        Log.d(TAG, "loaded " + rowList.size() + " rows into table");
    }


    @Override
    public Map<String, Map<String, QueryResponse>> getContent() {
        return rowList;
    }

    @Override
    public String[] getHeaderColumns() {
        Set<String> strings = getColumnDefinition().keySet();
        return strings.toArray(new String[]{});
    }

    /**
     * helper method to build row titles - if data is present
     *
     * @param row
     * @param columns
     * @param divider
     * @return
     */
    protected String concatenateIfPossible(Map<String, QueryResponse> row, String[] columns, String divider) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            String fieldOrEmpty = getFieldOrEmpty(row, columns[i]);
            if (!fieldOrEmpty.isEmpty()) {
                if (i != 0) {
                    sb.append(" ").append(divider).append(" ");
                }
                sb.append(fieldOrEmpty);
            }
        }
        return sb.toString();
    }

    /**
     * helper method to build titles
     *
     * @param row
     * @param fieldName
     * @return
     */
    protected String getFieldOrEmpty(Map<String, QueryResponse> row, String fieldName) {
        if (!row.containsKey(fieldName)) {
            return "";
        }
        QueryResponse qr = row.get(fieldName);
        if (qr != null) {
            return qr.getValue();
        }
        return "";
    }


    public abstract static class AbstractTableQueryRequest<T extends SnmpQuery> extends AbstractQueryRequest<T> {
        public AbstractTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public AbstractTableQueryRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public boolean isSingleRequest() {
            return false;
        }
    }
}
