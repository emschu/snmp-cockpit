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

package org.emschu.snmp.cockpit.query.impl.general;

import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;

import java.util.List;

import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.AbstractSnmpQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * first implementation of a query class
 */
public class SystemQuery extends AbstractSnmpQuery {

    private String sysDescr = null;
    private String sysObjectId = null;
    private String sysUpTime = null;
    private String sysContact = null;
    private String sysName = null;
    private String sysLocation = null;
    private String sysServices = null;

    @Override
    public void processResult(List<QueryResponse> results) {
        sysDescr = getOIDValue(results, SnmpConstants.sysDescr);
        sysObjectId = getOIDValue(results, SnmpConstants.sysObjectID);
        sysUpTime = getOIDValue(results, SnmpConstants.sysUpTime);
        sysContact = getOIDValue(results, SnmpConstants.sysContact);
        sysName = getOIDValue(results, SnmpConstants.sysName);
        sysLocation = getOIDValue(results, SnmpConstants.sysLocation);
        sysServices = getOIDValue(results, SnmpConstants.sysServices);
    }

    public String getSysDescr() {
        return sysDescr;
    }

    public String getSysObjectId() {
        return sysObjectId;
    }

    public String getSysUpTime() {
        return sysUpTime;
    }

    public String getSysContact() {
        return sysContact;
    }

    public String getSysName() {
        return sysName;
    }

    public String getSysLocation() {
        return sysLocation;
    }

    public String getSysServices() {
        return sysServices;
    }

    public static class SystemQueryRequest extends AbstractQueryRequest<SystemQuery> {

        public SystemQueryRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        @Override
        public boolean isSingleRequest() {
            return false;
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 1});
        }

        @Override
        public Class<SystemQuery> getQueryClass() {
            return SystemQuery.class;
        }
    }
}
