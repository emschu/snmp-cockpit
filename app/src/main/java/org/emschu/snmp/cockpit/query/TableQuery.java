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

import org.snmp4j.smi.OID;

import java.util.Map;

import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * table query interface to map fields one by one to a data structure
 */
public interface TableQuery extends SnmpQuery {
    /**
     * define columns as map
     * @return
     */
    public Map<String, OID> getColumnDefinition();

    /**
     * get header columns for table header
     *
     * @return
     */
    public String[] getHeaderColumns();

    /**
     * get processed content
     *
     * @return
     */
    public Map<String, Map<String, QueryResponse>> getContent();

    /**
     * method to generate a row title out of row data
     *
     * @param singleRow
     * @param index
     * @return
     */
    public String getRowTitle(Map<String, QueryResponse> singleRow, int index);
}
