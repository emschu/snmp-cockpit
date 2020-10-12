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

package org.emschu.snmp.cockpit.snmp;

import android.util.Log;


import org.jetbrains.annotations.NotNull;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * this adapter abstracts the (partially) version specific device/message handling
 * <p>
 */
public abstract class AbstractSnmpAdapter {

    public static final String TAG = AbstractSnmpAdapter.class.getName();
    protected Snmp snmp;
    protected TransportMapping<UdpAddress> udpAddressTransportMapping;
    protected AbstractTarget<UdpAddress> target = null;
    // can be set in subclasses
    protected DeviceConfiguration deviceConfiguration = null;

    /**
     * constructor
     *
     * @param snmp
     * @param udpAddressTransportMapping
     */
    public AbstractSnmpAdapter(Snmp snmp, TransportMapping<UdpAddress> udpAddressTransportMapping) {
        this.snmp = snmp;
        this.udpAddressTransportMapping = udpAddressTransportMapping;
    }

    // abstract methods

    /**
     * define version specific target in this class
     *
     * @return
     */
    @NotNull
    public abstract AbstractTarget<UdpAddress> getTarget();

    /**
     * method to query a single oid
     *
     * @param oid
     * @return
     */
    public abstract List<QueryResponse> querySingle(String oid);

    /**
     * method to query a walk of a single oid
     *
     * @param oid
     * @return
     */
    public abstract List<QueryResponse> queryWalk(String oid);

    /**
     * common used methods
     *
     * @param treeUtils
     * @param oid
     * @return
     * @throws NoSnmpResponseException
     */
    public synchronized List<QueryResponse> basicWalk(TreeUtils treeUtils, String oid) throws NoSnmpResponseException {
        Log.d(TAG, "query walk - " + oid);
        if (!isSnmpConnectionAllowed()) {
            Log.w(TAG, "no snmp connection allowed, execution prevented");
            return null;
        }
        if (!udpAddressTransportMapping.isListening()) {
            Log.wtf(TAG, "no transport mapping is listening!");
            return null;
        }

        final List<TreeEvent> events =
                Collections.synchronizedList(treeUtils.getSubtree(getTarget(), new OID(oid)));

        if (events.isEmpty()) {
            Log.w(TAG, "no response event returned");
            throw new NoSnmpResponseException();
        }
        Log.d(TAG, String.format("request returned %s responses", events.size()));

        synchronized (events) {
            List<QueryResponse> queryResponses = Collections.synchronizedList(new ArrayList<>());
            // this code is critical for ConcurrentModificationExceptions
            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    Log.e(TAG, "Error: table OID [" + oid + "] " + event.getErrorMessage());
                    continue;
                }

                VariableBinding[] varBindings = event.getVariableBindings();
                if (varBindings == null || varBindings.length == 0) {
                    Log.d(TAG, "no var bindings");
                    continue;
                }
                for (VariableBinding varBinding : varBindings) {
                    if (varBinding != null) {
                        queryResponses.add(new QueryResponse(varBinding.getOid().toDottedString(), varBinding));
                    }
                }
            }
            SnmpManager.getInstance().incrementRequestCounter();
            return queryResponses;
        }
    }

    public DeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }

    public void setDeviceConfiguration(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
    }

    /**
     * synchronous get
     *
     * @param pdu
     * @return
     */
    protected List<QueryResponse> basicSingleGet(PDU pdu) {
        List<QueryResponse> responseList = new ArrayList<>();

        if (!isSnmpConnectionAllowed()) {
            Log.e(TAG, "no snmp connection allowed");
            return responseList;
        }

        try {
            if (!udpAddressTransportMapping.isListening()) {
                Log.w(TAG, "Socket is no longer open!");
                return responseList;
            }
            ResponseEvent re = snmp.get(pdu, getTarget());
            boolean isResponseValid = checkResponseEvent(re);
            if (isResponseValid) {
                PDU response = re.getResponse();
                for (VariableBinding varBind : response.getVariableBindings()) {
                    responseList.add(new QueryResponse(varBind.getOid().toDottedString(), varBind));
                }
                SnmpManager.getInstance().incrementRequestCounter();
                return responseList;
            }
        } catch (IOException | NoSnmpResponseException e) {
            Log.w(AbstractSnmpAdapter.class.getName(), "exception message:" + e.getMessage());
            return new ArrayList<>();
        }
        return responseList;
    }

    /**
     * helper method to check if we are allowed to fire a snmp request
     *
     * @return
     */
    private boolean isSnmpConnectionAllowed() {
        if (!CockpitStateManager.getInstance().getNetworkSecurityObservable().getValue()) {
            Log.w(TAG, "request not allowed. network not secure!");
            return false;
        }
        return true;
    }

    /**
     * generic method to check if response event is valid
     *
     * @param re ResponseEvent
     * @return result
     * @throws NoSnmpResponseException
     */
    protected boolean checkResponseEvent(ResponseEvent re) throws NoSnmpResponseException {
        if (re != null) {
            PDU pdu = re.getResponse();
            if (pdu == null) {
                throw new NoSnmpResponseException();
            }
            Log.d(TAG, "pdu status: " + pdu.getErrorStatus() + " " + pdu.getErrorStatusText());
            return pdu.getErrorStatus() == PDU.noError;
        }
        return false;
    }

    /**
     * formats the address string for a connection
     *
     * @return
     */
    protected String getGenericAddress() {
        return String.format(
                "%s:%s/%s",
                deviceConfiguration.getNetworkProtocol(),
                deviceConfiguration.getTargetIp(),
                deviceConfiguration.getTargetPort()
        );
    }

    protected String getUdpAddress() {
        if (deviceConfiguration.isIpv6()) {
            return String.format("[%s]/%s",
                    deviceConfiguration.getTargetIp(),
                    deviceConfiguration.getTargetPort()
            );
        }
        return String.format("%s/%s",
                deviceConfiguration.getTargetIp(),
                deviceConfiguration.getTargetPort()
        );
    }
}
