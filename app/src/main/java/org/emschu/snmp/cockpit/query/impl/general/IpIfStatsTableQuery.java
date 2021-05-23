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

import org.emschu.snmp.cockpit.query.AbstractSnmpTableQuery;
import org.emschu.snmp.cockpit.query.OIDCatalog;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;
import org.snmp4j.smi.OID;

import java.util.HashMap;
import java.util.Map;

/**
 * this class represents a query for ip route table 1.3.6.1.2.1.4.31.3
 */
public class IpIfStatsTableQuery extends AbstractSnmpTableQuery {

    public static final String TAG = IpIfStatsTableQuery.class.getName();

    @Override
    public Map<String, OID> getColumnDefinition() {

        String[] columns = new String[] {
                "ipIfStatsIPVersion",
                "ipIfStatsIfIndex",
                "ipIfStatsInReceives",
                "ipIfStatsHCInReceives",
                "ipIfStatsInOctets",
                "ipIfStatsHCInOctets",
                "ipIfStatsInHdrErrors",
                "ipIfStatsInNoRoutes",
                "ipIfStatsInAddrErrors",
                "ipIfStatsInUnknownProtos",
                "ipIfStatsInTruncatedPkts",
                "ipIfStatsInForwDatagrams",
                "ipIfStatsHCInForwDatagrams",
                "ipIfStatsReasmReqds",
                "ipIfStatsReasmOKs",
                "ipIfStatsReasmFails",
                "ipIfStatsInDiscards",
                "ipIfStatsInDelivers",
                "ipIfStatsInDelivers",
                "ipIfStatsHCInDelivers",
                "ipIfStatsOutRequests",
                "ipIfStatsHCOutRequests",
                "ipIfStatsOutForwDatagrams",
                "ipIfStatsHCOutForwDatagrams",
                "ipIfStatsOutDiscards",
                "ipIfStatsOutFragReqds",
                "ipIfStatsOutFragOKs",
                "ipIfStatsOutFragFails",
                "ipIfStatsOutFragCreates",
                "ipIfStatsOutTransmits",
                "ipIfStatsHCOutTransmits",
                "ipIfStatsOutOctets",
                "ipIfStatsHCOutOctets",
                "ipIfStatsInMcastPkts",
                "ipIfStatsHCInMcastPkts",
                "ipIfStatsInMcastOctets",
                "ipIfStatsHCInMcastOctets",
                "ipIfStatsOutMcastPkts",
                "ipIfStatsHCOutMcastPkts",
                "ipIfStatsOutMcastOctets",
                "ipIfStatsHCOutMcastOctets",
                "ipIfStatsInBcastPkts",
                "ipIfStatsHCInBcastPkts",
                "ipIfStatsOutBcastPkts",
                "ipIfStatsHCOutBcastPkts",
                "ipIfStatsDiscontinuityTime",
                "ipIfStatsRefreshRate",
        };

        Map<String, OID> columnDefinition = new HashMap<>();

        OIDCatalog oidCatalog = OIDCatalog.getInstance(null);
        for (String asnName : columns) {
            columnDefinition.put(asnName, new OID(oidCatalog.getOidByAsn(asnName)));
        }

        return columnDefinition;
    }

    @Override
    public String getRowTitle(Map<String, QueryResponse> singleRow, int rowIndex) {
        return String.valueOf(rowIndex) + concatenateIfPossible(singleRow, new String[] {
                "ipIfStatsIfIndex",
                "ipIfStatsIPVersion"
        }, null);
    }

    /**
     * request class
     */
    public static class IpIfStatsTableRequest extends AbstractTableQueryRequest<IpIfStatsTableQuery> {
        public IpIfStatsTableRequest(DeviceConfiguration deviceConfiguration) {
            super(deviceConfiguration);
        }

        public IpIfStatsTableRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
            super(deviceConfiguration, contentTitleResourceId);
        }

        @Override
        public OID getOidQuery() {
            return new OID(new int[]{1, 3, 6, 1, 2, 1, 4, 31, 3});
        }

        @Override
        public Class<IpIfStatsTableQuery> getQueryClass() {
            return IpIfStatsTableQuery.class;
        }
    }
}
