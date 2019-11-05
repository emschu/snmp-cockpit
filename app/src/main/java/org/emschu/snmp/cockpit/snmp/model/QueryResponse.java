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

package org.emschu.snmp.cockpit.snmp.model;

import org.snmp4j.smi.VariableBinding;

/**
 * representing a real single snmp response of {@link org.emschu.snmp.cockpit.snmp.SnmpConnection}.
 * query with one oid.
 */
public class QueryResponse {
    private String oid;
    private VariableBinding variableBinding;

    /**
     * note: we should not store the whole pdu object, extract what you need in constructor to new class vars
     *
     * @param oid
     * @param variableBinding
     */
    public QueryResponse(String oid, VariableBinding variableBinding) {
        this.oid = oid;
        this.variableBinding = variableBinding;
    }

    public String getValue() {
        return this.variableBinding.getVariable().toString();
    }

    public String getOid() {
        return oid;
    }

    public VariableBinding getVariableBinding() {
        return variableBinding;
    }

    @Override
    public String toString() {
        return "QueryResponse{" +
                "oid='" + oid + '\'' +
                ", variableBinding=" + variableBinding +
                '}';
    }
}
