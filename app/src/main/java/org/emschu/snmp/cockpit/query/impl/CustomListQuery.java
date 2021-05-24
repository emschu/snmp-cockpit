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

package org.emschu.snmp.cockpit.query.impl;

import org.emschu.snmp.cockpit.persistence.model.CustomQuery;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.AbstractSnmpListQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.snmp4j.smi.OID;

/**
 * handle custom queries created by the user
 */
public class CustomListQuery extends AbstractSnmpListQuery {

    /**
     * snmp usage information
     */
    public static class CustomQueryRequest extends AbstractQueryRequest<CustomListQuery> {
        private final CustomQuery customQuery;

        public CustomQueryRequest(DeviceConfiguration deviceConfiguration, CustomQuery customQuery) {
            super(deviceConfiguration, 0);
            this.customQuery = customQuery;
        }

        @Override
        public boolean isSingleRequest() {
            return customQuery.isSingleQuery();
        }

        @Override
        public OID getOidQuery() {
            return new OID(customQuery.getOid());
        }

        @Override
        public Class<CustomListQuery> getQueryClass() {
            return CustomListQuery.class;
        }

        public CustomQuery getCustomQuery() {
            return customQuery;
        }
    }
}
