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

import org.emschu.snmp.cockpit.query.ListQuery;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * this class represents a ui section of a list query
 *
 */
public class ListQuerySection extends AbstractCockpitQuerySection {
    private final ListQuery abstractListQuery;
    private boolean collapsible = false;

    public ListQuerySection(String title, ListQuery abstractListQuery) {
        super(title, SectionType.LIST);
        this.abstractListQuery = abstractListQuery;
    }

    /**
     * constructor
     *
     * @param title
     * @param abstractListQuery
     * @param collapsible
     */
    public ListQuerySection(String title, ListQuery abstractListQuery, boolean collapsible) {
        super(title, SectionType.LIST);
        this.abstractListQuery = abstractListQuery;
        this.collapsible = collapsible;
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    /**
     * generate html of this component
     *
     * @return
     */
    @Override
    public String generateHtml() {
        StringBuilder sb = new StringBuilder();

        // collapsible list items have title in accordion row
        if (!isCollapsible()) {
            sb.append(generateTitle());
        }

        if (isEmpty()) {
            if (isCollapsible()) {
                sb.append(generateTitle());
            }
            // translate this as a string resource!
            addNoDataText(sb);
        } else {
            StringBuilder cb = new StringBuilder();
            cb.append("<ul>");
            for (QueryResponse queryResponse : abstractListQuery.getListItems()) {
                addQueryResponseRow(cb, queryResponse);
            }
            cb.append("</ul>");

            if (isCollapsible()) {
                addAccordionItem(sb, getTitle(), cb.toString(), "list_header");
            } else {
                sb.append("<div class='list'>");
                sb.append(cb.toString());
                sb.append("</div>");
            }
        }

        return sb.toString();
    }

    @Override
    public boolean isEmpty() {
        return abstractListQuery.getListItems().isEmpty();
    }
}
