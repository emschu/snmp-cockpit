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

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent;
import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;
import org.emschu.snmp.cockpit.tasks.QueryTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.COMMUNITY_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.ENC_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.HOST_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.IPV6_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.PORT_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.USER_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.USER_PASSPHRASE_KEY;

/**
 * this singleton class manages devices
 */
public class DeviceManager {

    public static final String TAG = DeviceManager.class.getName();
    private final List<DeviceMonitorItemContent.DeviceMonitorItem> fragmentItemList = Collections.synchronizedList(new ArrayList<>());
    private final List<ManagedDevice> managedDevices = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<String>> deviceTabs = new ConcurrentHashMap<>();

    private static DeviceManager instance;

    /**
     * private singleton constructor
     */
    private DeviceManager() {
//        add(new SnmpConfigurationFactory().createDummyV1Config("192.168.150.1", "public"), true);
    }

    /**
     * accessor method
     *
     * @return
     */
    public synchronized static DeviceManager getInstance() {
        if (instance == null) {
            instance = new DeviceManager();
        }
        return instance;
    }

    public List<DeviceMonitorItemContent.DeviceMonitorItem> getDeviceList() {
        return fragmentItemList;
    }

    /**
     * method to add a new snmp connection to the application
     *
     * @param deviceConfiguration
     */
    public synchronized void add(DeviceConfiguration deviceConfiguration, boolean isDummy) {
        String newDeviceId = "#" + (getDeviceList().size() + 1);
        Log.i(TAG, "add " + deviceConfiguration.getSnmpVersionEnum().toString()
                + " device to devicelist. user: " + deviceConfiguration.getUsername());
        // we need an instance of systemquery in order to show an item
        SystemQuery systemQuery;
        if (!isDummy) {
            QueryTask<SystemQuery> qt = new QueryTask<>();
            qt.executeOnExecutor(SnmpManager.getInstance().getThreadPoolExecutor(),
                    new SystemQuery.SystemQueryRequest(deviceConfiguration));
            systemQuery = qt.getQuery();
        } else {
            systemQuery = new SystemQuery();
        }
        if (systemQuery != null) {
            DeviceMonitorItemContent.DeviceMonitorItem deviceMonitorItem = new DeviceMonitorItemContent.DeviceMonitorItem(
                    newDeviceId,
                    deviceConfiguration.getTargetIp(),
                    String.valueOf(deviceConfiguration.getTargetPort()),
                    deviceConfiguration,
                    systemQuery
            );
            Log.d(TAG, "add new device item " + deviceMonitorItem.id);
            fragmentItemList.add(deviceMonitorItem);
            ManagedDevice managedDevice =
                    new ManagedDevice(newDeviceId, deviceMonitorItem, deviceConfiguration, systemQuery, isDummy);
            managedDevices.add(managedDevice);
            Log.d(TAG, "Managed devices: " + managedDevices.toString());
        } else {
            Log.e(TAG, "no system query response received! Skip adding device.");
        }
        CockpitStateManager.getInstance().getAreDevicesConnectedObservable().setValueAndTriggerObservers(!managedDevices.isEmpty());
    }

    /**
     * Create a {@link DeviceConfiguration} class out of login intent extra data
     * Keys defined in  {@link org.emschu.snmp.cockpit.activity.SNMPLoginActivity}
     *
     * @param data
     * @return
     */
    public DeviceConfiguration createDeviceConfiguration(Intent data) {
        if (data == null) {
            throw new IllegalArgumentException("null data given");
        }
        Log.d(TAG, "" + data.toString());
        String host = data.getStringExtra(HOST_KEY);
        String port = data.getStringExtra(PORT_KEY);
        String user = data.getStringExtra(USER_KEY);
        String community = data.getStringExtra(COMMUNITY_KEY);
        boolean isV1 = false;
        if (community != null && !community.isEmpty()) {
            // v1
            isV1 = true;
        }
        boolean isIpv6 = data.getBooleanExtra(IPV6_KEY, false);

        DeviceConfiguration deviceConfig;
        SnmpConfigurationFactory configurationFactory = new SnmpConfigurationFactory();
        if (isV1) {
            deviceConfig = configurationFactory.createSnmpV1Config(host, community);
        } else {
            String userAuthPassphrase = data.getStringExtra(USER_PASSPHRASE_KEY);
            String encPhrase = data.getStringExtra(ENC_KEY);
            deviceConfig = configurationFactory.createSnmpV3Config(host, user, userAuthPassphrase, encPhrase);
        }
        deviceConfig.setIpv6(isIpv6);

        // set proper port or fallback to 161
        int portInt;
        try {
            portInt = Integer.parseInt(String.valueOf(port));
        } catch (NumberFormatException nfe) {
            portInt = 161;
        }
        deviceConfig.setTargetPort(portInt);

        return deviceConfig;
    }

    public List<ManagedDevice> getManagedDevices() {
        return managedDevices;
    }

    /**
     * method to retrieve a managed device instance by its id
     * <p>
     * used in {@link org.emschu.snmp.cockpit.fragment.tabs.DeviceDetailFragment}
     *
     * @param deviceId
     * @return
     */
    public ManagedDevice getDevice(String deviceId) {
        for (ManagedDevice singleManagedDevice : getManagedDevices()) {
            if (singleManagedDevice
                    .getDeviceConfiguration().getUniqueDeviceId().equals(deviceId)) {
                return singleManagedDevice;
            }
        }
        return null;
    }

    /**
     * update all system queries.
     * should be avoided
     */
    public void updateSystemQueries() {
        Log.d(TAG, "updating system queries of all devices");

        for (ManagedDevice singleManagedDevice : managedDevices) {
            updateSystemQuery(singleManagedDevice);
        }
    }

    /**
     * update the system query of a managed device
     *
     * @param managedDevice
     */
    public void updateSystemQuery(ManagedDevice managedDevice) {
        DeviceConfiguration deviceConfiguration = managedDevice.getDeviceConfiguration();
        Log.d(TAG, "update system query of managed device "
                + deviceConfiguration.getUniqueDeviceId());

        int offset = deviceConfiguration.getAdditionalTimeoutOffset();
        final int toleranceFailureThreshold = 2;
        int toleranceFailureThresholdCounter = 0;
        while (toleranceFailureThresholdCounter <= toleranceFailureThreshold) {
            try {
                QueryTask<SystemQuery> systemQueryTask = new QueryTask<>();
                systemQueryTask.execute(new SystemQuery.SystemQueryRequest(deviceConfiguration));

                SystemQuery systemQuery = systemQueryTask
                        .get((long) CockpitPreferenceManager.TIMEOUT_WAIT_ASYNC_MILLISECONDS_SHORT + offset, TimeUnit.MILLISECONDS);
                if (systemQuery != null) {
                    Log.d(TAG, "system query updated");
                    managedDevice.updateSystemQuery(systemQuery);
                    SnmpManager.getInstance().resetTimeout(deviceConfiguration);
                    return;
                } else {
                    toleranceFailureThresholdCounter++;
                    Log.w(TAG, "no system query found");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                Log.w(TAG, "execution exception: " + e.getMessage());
            } catch (TimeoutException e) {
                Log.w(TAG, "timeout reached. skip update of managed device: " + deviceConfiguration.getUniqueDeviceId());
                toleranceFailureThresholdCounter++;

                if (toleranceFailureThresholdCounter >= toleranceFailureThreshold) {
                    SnmpManager.getInstance().registerTimeout(deviceConfiguration);
                    return;
                }
            }
        }
    }

    /**
     * method to clear all items. expensive
     */
    public synchronized void removeAllItems() {
        Log.d(TAG, "remove all devices requested");
        CockpitStateManager.getInstance().setRemovalOngoing(true);

        Iterator<ManagedDevice> iterator = managedDevices.iterator();
        // do not use a for loop here, we change the list!
        List<String> deviceIdList = new ArrayList<>();
        while (iterator.hasNext()) {
            ManagedDevice md = iterator.next();
            deviceIdList.add(md.getDeviceConfiguration().getUniqueDeviceId());
        }
        for (String deviceId : deviceIdList) {
            // remove every single device, but not on its own list
            removeItem(deviceId);
        }
        // should only clean up stuff
        SnmpManager.getInstance().clearConnections();

        fragmentItemList.clear();
        managedDevices.clear();
        CockpitStateManager.getInstance().getAreDevicesConnectedObservable().setValueAndTriggerObservers(false);
        CockpitStateManager.getInstance().setRemovalOngoing(false);
    }

    /**
     * remove a single device identified by the deviceId
     *
     * @param uniqueDeviceId
     */
    public synchronized void removeItem(String uniqueDeviceId) {
        if (uniqueDeviceId == null) {
            throw new IllegalArgumentException("unique device id is null");
        }
        Log.d(TAG, "remove item requested: " + uniqueDeviceId);
        CockpitStateManager.getInstance().setRemovalOngoing(true);

        for (DeviceMonitorItemContent.DeviceMonitorItem deviceItem : fragmentItemList) {
            if (uniqueDeviceId.equals(deviceItem.deviceConfiguration.getUniqueDeviceId())) {
                fragmentItemList.remove(deviceItem);
                Log.d(TAG, "item deleted: " + deviceItem.getDeviceConfiguration().getUniqueDeviceId());
                break;
            }
        }

        Iterator<ManagedDevice> deviceIterator = managedDevices.iterator();
        SnmpManager snmpManager = SnmpManager.getInstance();
        while (deviceIterator.hasNext()) {
            ManagedDevice managedDevice = deviceIterator.next();
            if (uniqueDeviceId.equals(managedDevice.getDeviceConfiguration().getUniqueDeviceId())) {
                Log.d(TAG, "device deleted: " + managedDevice.getDeviceConfiguration().getUniqueDeviceId());
                // this could cause network operations and must run async
                new Thread(() -> SnmpManager.getInstance().removeConnection(managedDevice.getDeviceConfiguration())).start();

                snmpManager.resetTimeout(managedDevice.getDeviceConfiguration());
                // clear cache immediately
                managedDevices.remove(managedDevice);
                CockpitStateManager.getInstance().getQueryCache()
                        .evictDeviceEntries(managedDevice.getDeviceConfiguration().getUniqueDeviceId());
                break;
            }
        }
        CockpitStateManager.getInstance().getAreDevicesConnectedObservable().setValueAndTriggerObservers(!managedDevices.isEmpty());
        CockpitStateManager.getInstance().setRemovalOngoing(false);
    }

    /**
     * async wrapper to request update
     *
     * @param managedDevice
     */
    public void updateSystemQueryAsync(ManagedDevice managedDevice) {
        new Handler(Looper.getMainLooper()).post(() -> updateSystemQuery(managedDevice));
    }

    /**
     * list with labels
     *
     * @return
     */
    public Map<String, String> getDisplayableDeviceList() {
        Map<String, String> indexedItemMap = new HashMap<>();
        for (ManagedDevice singleManagedDevice : managedDevices) {
            indexedItemMap.put(singleManagedDevice.getDeviceConfiguration().getUniqueDeviceId(),
                    singleManagedDevice.getDeviceLabel());
        }
        return indexedItemMap;
    }

    /**
     * adds a new tab with a single oid query to a device
     *
     * @param deviceId
     * @param oidQuery
     */
    public void addNewDeviceTab(String deviceId, String oidQuery) {
        List<String> tabList;
        if (deviceTabs.containsKey(deviceId)) {
            tabList = deviceTabs.get(deviceId);
        } else {
            tabList = new ArrayList<>();
            deviceTabs.put(deviceId, tabList);
        }
        if (tabList != null) {
            tabList.add(oidQuery);
        }
    }

    /**
     * method to get stored tabs
     *
     * @param deviceId
     * @return
     */
    public List<String> getTabs(String deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("invalid null deviceId");
        }
        if (deviceTabs.containsKey(deviceId)) {
            return deviceTabs.get(deviceId);
        }
        return null;
    }

    /**
     * @param deviceIdKey
     * @return
     */
    public boolean hasDevice(String deviceIdKey) {
        for (ManagedDevice managedDevice : managedDevices) {
            if (managedDevice.getDeviceConfiguration().getUniqueDeviceId().equals(deviceIdKey)) {
                return true;
            }
        }
        Log.w(TAG, "device " + deviceIdKey + " is not found");
        return false;
    }

    /**
     * Method to check if there is an open snmp v2c connection
     *
     * @return
     */
    public boolean hasV2Connection() {
        for (ManagedDevice md : managedDevices) {
            if (md.getDeviceConfiguration().isV2c()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to check if there is an open snmp v1 connection
     *
     * @return
     */
    public boolean hasV1Connection() {
        for (ManagedDevice md : managedDevices) {
            if (md.getDeviceConfiguration().isV1()) {
                return true;
            }
        }
        return false;
    }
}
