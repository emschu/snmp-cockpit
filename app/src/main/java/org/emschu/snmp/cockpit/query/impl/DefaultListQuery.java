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

import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.AbstractSnmpListQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.snmp4j.smi.OID;

/**
 * Every OID-Query of this app can be displayed in a list
 */
public class DefaultListQuery extends AbstractSnmpListQuery {

    /**
     * snmp usage information
     */
    public static class SnmpUsageQueryRequest extends AbstractQueryRequest<DefaultListQuery> {

        public SnmpUsageQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public boolean isSingleRequest() {
            return false;
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 11});
        }

        @Override
        public Class<DefaultListQuery> getQueryClass() {
            return DefaultListQuery.class;
        }
    }

    /**
     * ip section
     */
    public static class IpSectionQueryRequest extends AbstractQueryRequest<DefaultListQuery> {

        public IpSectionQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public IpSectionQueryRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public boolean isSingleRequest() {
            return false;
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 4});
        }

        @Override
        public Class<DefaultListQuery> getQueryClass() {
            return DefaultListQuery.class;
        }
    }

    /**
     * mibs
     */
    public static class MrTableQueryRequest extends AbstractQueryRequest<DefaultListQuery> {

        public MrTableQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public boolean isSingleRequest() {
            return false;
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 4, 1, 2021, 102});
        }

        @Override
        public Class getQueryClass() {
            return DefaultListQuery.class;
        }
    }
}
