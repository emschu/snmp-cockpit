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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.query.impl.bsd.SensorTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.HrDeviceTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.HrDiskStorageTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.HrPartitionTableQuery;
import org.emschu.snmp.cockpit.query.impl.ucdavis.DskTableQuery;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * this class is called when detail info of a device is requested
 */
public class HardwareQueryTask extends AsyncTask<Void, Void, Void> implements TabTaskHelper {

    public static final String TAG = HardwareQueryTask.class.getName();

    private final AtomicReference<CockpitQueryView> queryView = new AtomicReference<>();
    private DeviceConfiguration deviceConfiguration;
    // query tasks
    private QueryTask<SensorTableQuery> sensorTableQueryTask;
    private QueryTask<DskTableQuery> dskQueryTask;
    private QueryTask<HrDeviceTableQuery> hrDeviceQueryTask;
    private QueryTask<HrDiskStorageTableQuery> hrDiskStorageQueryTask;
    private QueryTask<HrPartitionTableQuery> hrPartitionTableQueryTask;

    /**
     * constructor
     *
     * @param queryView
     * @param deviceConfiguration
     */
    public HardwareQueryTask(CockpitQueryView queryView, DeviceConfiguration deviceConfiguration) {
        this.queryView.set(queryView);
        this.deviceConfiguration = deviceConfiguration;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "entering hardware query task");
        super.onPreExecute();

        ThreadPoolExecutor tpe = SnmpManager.getInstance().getThreadPoolExecutor();

        if (tpe.isTerminating() || tpe.isShutdown() || tpe.isTerminated()) {
            Log.d(TAG, "invalid thread pool given");
            return;
        }
        // start and send queries
        sensorTableQueryTask = new QueryTask<>();
        sensorTableQueryTask.executeOnExecutor(tpe, new SensorTableQuery.SensorTableQueryRequest(deviceConfiguration));
        dskQueryTask = new QueryTask<>();
        dskQueryTask.executeOnExecutor(tpe, new DskTableQuery.DskTableQueryRequest(deviceConfiguration));
        hrDeviceQueryTask = new QueryTask<>();
        hrDeviceQueryTask.executeOnExecutor(tpe, new HrDeviceTableQuery.HrDeviceTableRequest(deviceConfiguration));
        hrDiskStorageQueryTask = new QueryTask<>();
        hrDiskStorageQueryTask.executeOnExecutor(tpe, new HrDiskStorageTableQuery.HrDiskStorageTableRequest(deviceConfiguration));
        hrPartitionTableQueryTask = new QueryTask<>();
        hrPartitionTableQueryTask.executeOnExecutor(tpe, new HrPartitionTableQuery.HrPartitionTableRequest(deviceConfiguration));
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (deviceConfiguration == null) {
            throw new IllegalArgumentException("null DeviceConfiguration given");
        }
        Context context = queryView.get().getContext();
        if (context == null) {
            throw new IllegalArgumentException("null context given");
        }

        addTableSection(context.getString(R.string.hw_info_task_view_label_table_sensortable), sensorTableQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_dsktable), dskQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_hrdevicetable), hrDeviceQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_hrdiskstorage), hrDiskStorageQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_hrpartitiontable), hrPartitionTableQueryTask, queryView);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG, "start rendering");
        // here we are on the ui thread
        queryView.get().render(true);
    }

    @Override
    public void cancelTasks() {
        sensorTableQueryTask.cancel(true);
        dskQueryTask.cancel(true);
        hrDeviceQueryTask.cancel(true);
        hrDiskStorageQueryTask.cancel(true);
        hrPartitionTableQueryTask.cancel(true);
    }
}