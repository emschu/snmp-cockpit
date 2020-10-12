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

package org.emschu.snmp.cockpit.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import androidx.recyclerview.widget.RecyclerView;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent;
import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * simple async task to refresh list async way
 * <p>
 */
public class RefreshListTask extends AsyncTask<Void, Void, Void> {

    public static final String TAG = RefreshListTask.class.getName();

    private final ArrayList<QueryTask<SystemQuery>> queryTaskList = new ArrayList<>();
    private final AtomicReference<RecyclerView> recyclerViewAtomicReference = new AtomicReference<>();

    public RefreshListTask(RecyclerView recyclerView) {
        recyclerViewAtomicReference.set(recyclerView);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // iterate on copy
        final List<DeviceMonitorItemContent.DeviceMonitorItem> deviceList = new ArrayList<>(DeviceManager.getInstance().getDeviceList());
        for (DeviceMonitorItemContent.DeviceMonitorItem deviceItem : deviceList) {
            QueryTask<SystemQuery> qt = new QueryTask<>();
            qt.executeOnExecutor(SnmpManager.getInstance().getThreadPoolExecutor(),
                    new SystemQuery.SystemQueryRequest(deviceItem.deviceConfiguration));
            queryTaskList.add(qt);
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ArrayList<DeviceMonitorItemContent.DeviceMonitorItem> deviceList = new ArrayList<>();
        // copy data to be avoid concurrent modification exceptions
        List<DeviceMonitorItemContent.DeviceMonitorItem> tmpDeviceList = new ArrayList<>(DeviceManager.getInstance().getDeviceList());
        int counter = 1;
        for (DeviceMonitorItemContent.DeviceMonitorItem deviceItem : tmpDeviceList) {
            SystemQuery systemQuery = null;
            if (!deviceItem.deviceConfiguration.isDummy()) {
                for (QueryTask<?> qt : queryTaskList) {
                    if (deviceItem.getDeviceConfiguration().getUniqueDeviceId().equals(qt.getDeviceConfiguration().getUniqueDeviceId())) {
                        systemQuery = (SystemQuery) qt.getQuery();
                    }
                }
            }
            if (systemQuery == null) {
                Log.w(TAG, "no system query retrievable!");
                // use old as fallback
                systemQuery = deviceItem.systemQuery;
                if (!CockpitStateManager.getInstance().isInTimeouts()
                        && !CockpitStateManager.getInstance().isConnecting()) {
                    if (deviceItem.deviceConfiguration.getSnmpVersion() < 3) {
                        SnmpManager.getInstance().resetV1Connection(deviceItem.deviceConfiguration);
                    } else {
                        SnmpManager.getInstance().resetV3Connection(deviceItem.deviceConfiguration);
                    }
                }
            }
            deviceList.add(new DeviceMonitorItemContent.DeviceMonitorItem("#" + counter, deviceItem.host, deviceItem.port,
                    deviceItem.deviceConfiguration, systemQuery));
            counter++;
        }
        Log.d(TAG, "re-init device list: " + deviceList.toString());


        if (DeviceManager.getInstance().getDeviceList().size() == deviceList.size()) {
            DeviceManager.getInstance().getDeviceList().clear();
            DeviceManager.getInstance().getDeviceList().addAll(deviceList);
        } else {
            Log.w(TAG, "prevent refreshing device list");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        recyclerViewAtomicReference.get().getAdapter().notifyDataSetChanged();
    }
}