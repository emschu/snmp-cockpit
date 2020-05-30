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

package org.emschu.snmp.cockpit.snmp.adapter;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeUtils;

import java.util.List;

import org.emschu.snmp.cockpit.snmp.AbstractSnmpAdapter;
import org.emschu.snmp.cockpit.snmp.NoSnmpResponseException;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * this class handles snmp v1 specific stuff
 */
public class V1Adapter extends AbstractSnmpAdapter {

    public static final String TAG = V1Adapter.class.getName();

    private boolean isV1 = false;

    /**
     * constructor
     *  @param snmp
     * @param udpAddressTransportMapping
     * @param b
     */
    public V1Adapter(Snmp snmp, TransportMapping<UdpAddress> udpAddressTransportMapping, boolean isV1) {
        super(snmp, udpAddressTransportMapping);
        this.isV1 = isV1;
    }

    @NotNull
    @Override
    public AbstractTarget<UdpAddress> getTarget() {
        if (target != null) {
            return target;
        }
        CommunityTarget<UdpAddress> communityTarget = new CommunityTarget<>();
        OctetString securityName = new OctetString(deviceConfiguration.getUsername());
        communityTarget.setCommunity(securityName);
        communityTarget.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV); // this is fix for v1!

        communityTarget.setAddress(new UdpAddress(getAddress()));
        communityTarget.setVersion(deviceConfiguration.getSnmpVersion());
        communityTarget.setRetries(deviceConfiguration.getRetries());
        communityTarget.setTimeout(deviceConfiguration.getTimeout());
        target = communityTarget;
        return communityTarget;
    }

    @Override
    public List<QueryResponse> querySingle(String oid) {
        Log.d(TAG, "query single: " + oid);
        PDU pdu;
        if (isV1) {
            pdu = DefaultPDUFactory.createPDU(1);
        } else {
            pdu = DefaultPDUFactory.createPDU(2);
        }
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GETNEXT);
        return basicSingleGet(pdu);
    }

    @Override
    public List<QueryResponse> queryWalk(String oid) {
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        try {
            return basicWalk(treeUtils, oid);
        } catch (NoSnmpResponseException e) {
            Log.w(TAG, "no snmp v1 response for oid " + oid);
            return null;
        }
    }
}
