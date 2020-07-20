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

package org.emschu.snmp.cockpit;

import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.OctetString;

import org.emschu.snmp.cockpit.query.QueryCache;
import org.emschu.snmp.cockpit.tasks.SNMPConnectivityAddDeviceTask;
import org.emschu.snmp.cockpit.util.BooleanObservable;

/**
 * activities are coming and going - this class should last to hold the (network security) state of the app
 */
public class CockpitStateManager {
    private static CockpitStateManager instance;
    // observables
    private final BooleanObservable networkSecurityBooleanObservable = new BooleanObservable(false);
    private final BooleanObservable isInTimeoutsObservable = new BooleanObservable(false);
    private final BooleanObservable isInSessionTimeoutObservable = new BooleanObservable(false);

    private boolean isConnecting = false;
    private boolean isInRemoval = false;
    private boolean isInTestMode = false;
    private SNMPConnectivityAddDeviceTask connectionTask = null;
    private OctetString localEngineId = null;
    private final QueryCache queryCache = new QueryCache();

    /**
     * singleton access method
     *
     * @return
     */
    public static synchronized CockpitStateManager getInstance() {
        if (instance == null) {
            instance = new CockpitStateManager();
        }
        return instance;
    }

    public BooleanObservable getNetworkSecurityObservable() {
        return networkSecurityBooleanObservable;
    }

    public BooleanObservable getIsInTimeoutsObservable() {
        return isInTimeoutsObservable;
    }

    public BooleanObservable getIsInSessionTimeoutObservable() {
        return isInSessionTimeoutObservable;
    }

    public void setConnecting(boolean connecting) {
        isConnecting = connecting;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public SNMPConnectivityAddDeviceTask getConnectionTask() {
        return connectionTask;
    }

    public void setConnectionTask(SNMPConnectivityAddDeviceTask connectionTask) {
        this.connectionTask = connectionTask;
    }

    /**
     * get app wide unique local engine id
     *
     * @return
     */
    public OctetString getLocalEngineId() {
        if (localEngineId == null) {
            localEngineId = new OctetString(MPv3.createLocalEngineID());
        }
        return localEngineId;
    }

    public QueryCache getQueryCache() {
        return queryCache;
    }

    /**
     * helper method to detect timeout state
     * 
     * @return
     */
    public boolean isInTimeouts() {
        return getIsInTimeoutsObservable().getValue() || getIsInSessionTimeoutObservable().getValue();
    }

    public void setRemovalOngoing(boolean isInRemoval) {
        this.isInRemoval = isInRemoval;
    }

    public boolean isInRemoval() {
        return isInRemoval;
    }

    public boolean isInTestMode() {
        return isInTestMode;
    }

    public void setInTestMode(boolean inTestMode) {
        isInTestMode = inTestMode;
    }
}
