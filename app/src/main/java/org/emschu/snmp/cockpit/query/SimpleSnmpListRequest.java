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

import org.emschu.snmp.cockpit.query.impl.DefaultListQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;

/**
 * You can do any recursive snmp query with this reqest class.
 * Note: This query is not cacheable!
 *
 */
public class SimpleSnmpListRequest extends AbstractQueryRequest<DefaultListQuery> {
    private OID oid;

    public SimpleSnmpListRequest(DeviceConfiguration deviceConfiguration, String oid) {
        super(deviceConfiguration);
        this.oid = new OID(oid);
    }

    @Override
    public boolean isSingleRequest() {
        return false;
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public OID getOidQuery() {
        return oid;
    }

    @Override
    public Class<DefaultListQuery> getQueryClass() {
        return DefaultListQuery.class;
    }
}
