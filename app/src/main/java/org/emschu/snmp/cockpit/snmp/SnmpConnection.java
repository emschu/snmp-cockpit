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

import android.net.TrafficStats;
import android.util.Log;

import androidx.annotation.NonNull;

import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.snmp.adapter.V1Adapter;
import org.emschu.snmp.cockpit.snmp.adapter.V3Adapter;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthHMAC128SHA224;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthHMAC256SHA384;
import org.snmp4j.security.AuthHMAC384SHA512;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.security.nonstandard.PrivAES192With3DESKeyExtension;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * this class represents a real connection to an snmp daemon
 */
public class SnmpConnection {

    public static final String TAG = SnmpConnection.class.getName();
    private final DeviceConfiguration deviceConfiguration;

    private Snmp snmp;
    private DefaultUdpTransportMapping transport;
    private MessageDispatcher dispatcher;
    private AbstractSnmpAdapter adapter = null;

    // device specific:
    private final OctetString localEngineId = CockpitStateManager.getInstance().getLocalEngineId();
    private static boolean isInited = false;
    private static USM usm;
    private static int engineBoots = -1;
    private static final int SOCKET_ID = 10000;


    /**
     * for single connections
     *
     * @param deviceConfiguration
     */
    public SnmpConnection(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
        init();
    }

    /**
     * is called in constructor and sets up a snmp connection
     */
    private void init() {
        if (!isInited) {
            snmp4JSettings();
            isInited = true;
        }

        // load generic adapter stuff
        loadAdapters();

        Log.d(TAG, "using local engine id: " + localEngineId);

        if (transport == null || snmp == null) {
            Log.e(TAG, "no valid transport object");
            return;
        }

        if (!this.deviceConfiguration.isDummy()) {
            startListening();
            Log.d(TAG, "is listening: " + transport.isListening());
        }
    }

    /**
     * this is executed only once per app lifetime
     */
    private void snmp4JSettings() {
        Log.d(TAG, "snmp4j is initially configured");
        SNMP4JSettings.setAllowSNMPv2InV1(true);
        SNMP4JSettings.setEnterpriseID(1311);
        SNMP4JSettings.setMaxEngineIdCacheSize(100000); // 4 MB
        SNMP4JSettings.setThreadJoinTimeout(90000);
        SNMP4JSettings.setSnmp4jStatistics(SNMP4JSettings.Snmp4jStatistics.basic);
        SNMP4JSettings.setCheckUsmUserPassphraseLength(true);
        SNMP4JSettings.setForwardRuntimeExceptions(false);
        SNMP4JSettings.setExtensibilityEnabled(false);
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
        final SecurityProtocols instance = SecurityProtocols.getInstance();
        instance.addPrivacyProtocol(new PrivDES());
        instance.addPrivacyProtocol(new Priv3DES());
        instance.addPrivacyProtocol(new PrivAES128());
        instance.addPrivacyProtocol(new PrivAES192());
        instance.addPrivacyProtocol(new PrivAES256());
        instance.addPrivacyProtocol(new PrivAES192With3DESKeyExtension());
        instance.addAuthenticationProtocol(new AuthSHA());
        instance.addAuthenticationProtocol(new AuthHMAC128SHA224());
        instance.addAuthenticationProtocol(new AuthHMAC192SHA256());
        instance.addAuthenticationProtocol(new AuthHMAC256SHA384());
        instance.addAuthenticationProtocol(new AuthHMAC384SHA512());
        instance.addAuthenticationProtocol(new AuthMD5());
    }

    /**
     * the transport instance
     *
     * @return
     */
    public TransportMapping<UdpAddress> getTransport() {
        return transport;
    }

    /**
     * direct access should be avoided
     *
     * @return
     */
    protected Snmp getSnmp() {
        return snmp;
    }

    /**
     * checks if a device configuration is registered in this connection
     *
     * @param deviceConfiguration
     * @return
     */
    private boolean isRegistered(DeviceConfiguration deviceConfiguration) {
        if (adapter != null
                && adapter.deviceConfiguration.getUniqueDeviceId()
                .equals(deviceConfiguration.getUniqueDeviceId())) {
            return true;
        }
        Log.e(TAG, "requested device configuration not registered!");
        return false;
    }

    /**
     * method to query single oids.
     * NOTE: sometimes you should append ".0" to avoid noSuchName Err
     *
     * @param deviceConfiguration
     * @param oid
     * @return
     */
    public List<QueryResponse> querySingle(DeviceConfiguration deviceConfiguration, String oid) {
        return querySingle(deviceConfiguration, new OID(oid));
    }

    /**
     * method to retrieve device specific version adapters
     *
     * @param deviceConfiguration
     * @return
     */
    private AbstractSnmpAdapter getAdapter(DeviceConfiguration deviceConfiguration) {
        if (adapter != null) {
            return adapter;
        }
        throw new IllegalStateException("invalid call for adapter");
    }

    /**
     * query a single oid
     *
     * @param deviceConfiguration
     * @param oid
     * @return
     */
    public List<QueryResponse> querySingle(DeviceConfiguration deviceConfiguration, OID oid) {
        if (!isRegistered(deviceConfiguration)) {
            Log.e(TAG, "device configuration not registered during querySingle call");
            return null;
        }
        if (!isSnmpAllowed()) {
            Log.w(TAG, "no snmp connection is allowed because of unsafe network");
            return null;
        }
        return this.adapter.querySingle(oid.toDottedString());
    }

    /**
     * method to walk an oid subtree
     *
     * @param deviceConfiguration
     * @param oid
     * @return
     */
    public List<QueryResponse> queryWalk(DeviceConfiguration deviceConfiguration, String oid) {
        return queryWalk(deviceConfiguration, new OID(oid));
    }

    /**
     * query walk
     *
     * @param deviceConfiguration
     * @param oid
     * @return
     */
    public List<QueryResponse> queryWalk(DeviceConfiguration deviceConfiguration, OID oid) {
        if (!isRegistered(deviceConfiguration)) {
            Log.e(TAG, "device configuration not registered during queryWalk call");
            return null;
        }
        if (!isSnmpAllowed()) {
            Log.w(TAG, "no snmp connection is allowed because of unsafe network");
            return null;
        }
        return this.adapter.queryWalk(oid.toDottedString());
    }

    public DeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }

    /**
     * simple debug method
     *
     * @param oid
     */
    public void printWalk(DeviceConfiguration deviceConfiguration, String oid) {
        List<QueryResponse> queryResponses = queryWalk(deviceConfiguration, oid);
        for (QueryResponse qr : queryResponses) {
            Log.d(TAG, qr.toString());
        }
    }

    /**
     * helper method to check if snmp connections are allowed
     *
     * @return
     */
    private boolean isSnmpAllowed() {
        boolean isNetworkSecure = CockpitStateManager.getInstance().getNetworkSecurityObservable().getValue();
        boolean isInTimeouts = CockpitStateManager.getInstance().getIsInTimeoutsObservable().getValue();
        boolean isInSessionTimeout = CockpitStateManager.getInstance().getIsInSessionTimeoutObservable().getValue();
        Log.d(TAG, "network security: " + isNetworkSecure);
        Log.d(TAG, "timeout state: " + isInTimeouts);
        Log.d(TAG, "session timeout state: " + isInTimeouts);

        return isNetworkSecure && !isInTimeouts && !isInSessionTimeout;
    }

    /**
     * closes transport + snmp
     */
    public void close() {
        Log.d(TAG, "stop listening on: " + transport.getListenAddress());
        try {
            if (snmp != null) {
                snmp.close();
            }
            transport.close();
            dispatcher = null;
            snmp = null;
        } catch (IOException | RuntimeException e) {
            Log.e(SnmpConnection.class.getName(), "exception message: " + e.getMessage());
        } finally {
            Log.d(TAG, "closing snmp connection finished");
        }
    }

    /**
     * load correct adapter depending on version
     */
    private synchronized void loadAdapters() {
        startupSnmp();

        // fill adapter and target list
        List<UsmUser> userList = new ArrayList<>();
        AbstractSnmpAdapter snmpAdapter = null;
        if (this.deviceConfiguration.isV3()) {
            // we use one single v3 adapter
            snmpAdapter = new V3Adapter(snmp, transport);
        }
        if (this.deviceConfiguration.isV1()) {
            snmpAdapter = new V1Adapter(snmp, transport, true);
        }
        if (this.deviceConfiguration.isV2c()) {
            snmpAdapter = new V1Adapter(snmp, transport, false);
        }
        if (snmpAdapter == null) {
            throw new IllegalStateException("adapter loading failed: " + this.deviceConfiguration.getSnmpVersion());
        }
        snmpAdapter.setDeviceConfiguration(this.deviceConfiguration);

        if (snmpAdapter instanceof V3Adapter) {
            UsmUser user = ((V3Adapter) snmpAdapter).getUser();
            if (user != null) {
                userList.add(user);
            } else {
                Log.e(TAG, "invalid user configuration detected");
            }
        }

        // setup usm
        if (!userList.isEmpty()) {
            setupUsm(userList);
        }

        this.adapter = snmpAdapter;
    }

    /**
     * init transport + snmp class of this connection
     */
    @SuppressWarnings("squid:S1313")
    private void startupSnmp() {
        if (transport == null) {
            try {
                TrafficStats.setThreadStatsTag(SOCKET_ID);
                transport = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/0"), false);
                transport.setAsyncMsgProcessingSupported(false);
                transport.setSocketTimeout(5000);
            } catch (IOException e) {
                Log.e(TAG, "exception during transport startup: " + e.getMessage());
            } catch (RuntimeException re) {
                Log.e(TAG, "runtime exception during snmp startup: " + re.getMessage());
            }
        }
        if (dispatcher == null) {
            dispatcher = getMessageDispatcher();
        }
        snmp = new Snmp(dispatcher, transport);
    }

    /**
     * configure snmp4j message dispatcher
     *
     * @return
     */
    public MessageDispatcher getMessageDispatcher() {
        if (dispatcher == null) {
            dispatcher = new MessageDispatcherImpl();
            dispatcher.addMessageProcessingModel(new MPv1());
            dispatcher.addMessageProcessingModel(new MPv2c());
            dispatcher.addMessageProcessingModel(new MPv3());
        }
        return dispatcher;
    }

    /**
     * simulates a "ping"
     *
     * @return
     */
    public boolean canPing(DeviceConfiguration deviceConfiguration) {
        if (deviceConfiguration.getLastPingTime() != 0 &&
                (System.currentTimeMillis() - deviceConfiguration.getLastPingTime()) < 7500L) {
            Log.d(TAG, "ping request not needed, there is a recent one");
            return true;
        }
        final int threshold = 2;
        int thresholdCounter = 0;
        while (thresholdCounter <= threshold) {
            // use sysName
            List<QueryResponse> responseList = querySingle(deviceConfiguration, "1.3.6.1.2.1.1.5.0");
            if (responseList == null) {
                Log.d(TAG, "ping: false - response list is empty");
                thresholdCounter++;
                continue;
            }
            if (!checkResponseList(responseList)) {
                Log.w(TAG, "ping: false - snmp connection error");
                thresholdCounter++;
                continue;
            }

            Log.d(TAG, "ping responseList (should be sysName):" + responseList.toString());
            boolean canPing = !responseList.isEmpty();
            Log.d(TAG, "ping: " + canPing);
            if (canPing) {
                deviceConfiguration.setLastPingTime(System.currentTimeMillis());
                return true;
            }
            thresholdCounter++;
        }
        return false;
    }

    /**
     * check for wrong user
     *
     * @param responseList
     * @return
     */
    private boolean checkResponseList(List<QueryResponse> responseList) {
        for (QueryResponse singleResponse : responseList) {
            if (singleResponse.getVariableBinding().getOid().equals(SnmpConstants.usmStatsUnknownUserNames)) {
                Log.d(TAG, "wrong username detected in ping");
                // unknown user detected!
                return false;
            }
        }
        return true;
    }

    /**
     * checks transport is listening
     */
    public void startListening() {
        try {
            if (!transport.isListening()) {
                snmp.listen();
                Log.d(TAG, "transport starts listening on udp socket: " + transport.getListenAddress().toString());
                return;
            }
            Log.d(TAG, "transport already listening at: " + transport.getListenAddress().toString());
        } catch (IOException ioException) {
            Log.e(TAG, "problem with connection socket: " + ioException.getMessage());
        }
    }

    /**
     * helper method to control usm
     *
     * @param userList
     */
    public void setupUsm(@NonNull List<UsmUser> userList) {
        Log.d(TAG, "usm handling enabled - v3 connection exists");
        if (usm != null) {
            Log.d(TAG, "unregistering old security model");
            SecurityModels.getInstance().removeSecurityModel(new Integer32(usm.getID()));
        }
        usm = new USM(SecurityProtocols.getInstance(), localEngineId, engineBoots++);
        Log.d(TAG, "engine boots: " + engineBoots);
        for (UsmUser singleUser : userList) {
            usm.addUser(singleUser);
        }
        Log.d(TAG, "usm: added " + userList.size() + " users");
        SecurityModels.getInstance().addSecurityModel(usm);
    }
}
