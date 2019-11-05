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

package org.emschu.snmp.cockpit.query.view;

import android.util.Log;

import java.util.Map;

import org.emschu.snmp.cockpit.query.TableQuery;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * generates an accordion view section
 */
public class GroupedListQuerySection extends AbstractCockpitQuerySection {

    private final TableQuery tableQuery;

    public GroupedListQuerySection(String title, TableQuery tableQuery) {
        super(title, SectionType.GROUPED_LIST);
        this.tableQuery = tableQuery;
    }

    @Override
    public String generateHtml() {
        StringBuilder sb = new StringBuilder();

        sb.append(generateTitle());

        if (isEmpty()) {
            // translate this as a string resource!
            addNoDataText(sb);
        }

        int i = 0;
        for (String rowIndex : tableQuery.getContent().keySet()) {
            Map<String, QueryResponse> stringQueryResponseMap = tableQuery.getContent().get(rowIndex);
            if (stringQueryResponseMap == null) {
                Log.w(TAG, "null content for rowIndex " + rowIndex);
                continue;
            }

            String rowTitle = tableQuery.getRowTitle(stringQueryResponseMap, i);
            StringBuilder cb = new StringBuilder();
            for (QueryResponse qr : stringQueryResponseMap.values()) {
                addQueryResponseRow(cb, qr);
            }
            addAccordionItem(sb, rowTitle, cb.toString(), null);
            i++;
        }

        return sb.toString();
    }

    @Override
    public boolean isEmpty() {
        return tableQuery.getContent().keySet().isEmpty();
    }
}
