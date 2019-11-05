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

import java.util.List;

import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * every snmp query class should derive from this superclass
 */
public abstract class AbstractSnmpQuery implements SnmpQuery {
    @Override
    public abstract void processResult(List<QueryResponse> results);

    /**
     * internal helper method to retrieve an oid out of the result list or return null
     *
     * @param results
     * @param oid
     * @return
     */
    protected String getOIDValue(List<QueryResponse> results, OID oid) {
        for (QueryResponse queryResponse : results) {
            if (queryResponse.getVariableBinding().getOid().equals(oid)) {
                return queryResponse.getVariableBinding().getVariable().toString();
            }
        }
        return null;
    }

}
