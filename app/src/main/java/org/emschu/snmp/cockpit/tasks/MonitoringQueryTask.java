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
import org.emschu.snmp.cockpit.query.impl.general.IcmpStatsTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpIfStatsTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpSystemStatsTableQuery;
import org.emschu.snmp.cockpit.query.impl.ucdavis.LaTableQuery;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * this class is called when detail info of a device is requested
 */
public class MonitoringQueryTask extends AsyncTask<Void, Void, Void> implements TabTaskHelper {

    public static final String TAG = MonitoringQueryTask.class.getName();

    private final AtomicReference<CockpitQueryView> queryView = new AtomicReference<>();
    private final DeviceConfiguration deviceConfiguration;
    private QueryTask<LaTableQuery> laTableQueryTask;
    private QueryTask<IpSystemStatsTableQuery> ipSystemStatsQueryTask;
    private QueryTask<IpIfStatsTableQuery> ipIfStatsQueryTask;
    private QueryTask<IcmpStatsTableQuery> icmpStatsQueryTask;

    /**
     * constructor
     *
     * @param queryView
     * @param deviceConfiguration
     */
    public MonitoringQueryTask(CockpitQueryView queryView, DeviceConfiguration deviceConfiguration) {
        this.queryView.set(queryView);
        this.deviceConfiguration = deviceConfiguration;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "entering monitoring query task");

        super.onPreExecute();

        ThreadPoolExecutor tpe = SnmpManager.getInstance().getThreadPoolExecutor();
        if (tpe.isTerminating() || tpe.isShutdown() || tpe.isTerminated()) {
            Log.d(TAG, "invalid thread pool given");
            return;
        }
        // start and send queries
        laTableQueryTask = new QueryTask<>();
        laTableQueryTask.executeOnExecutor(tpe, new LaTableQuery.LaTableQueryRequest(deviceConfiguration));
        ipSystemStatsQueryTask = new QueryTask<>();
        ipSystemStatsQueryTask.executeOnExecutor(tpe, new IpSystemStatsTableQuery.IpSystemStatsTableRequest(deviceConfiguration));
        ipIfStatsQueryTask = new QueryTask<>();
        ipIfStatsQueryTask.executeOnExecutor(tpe, new IpIfStatsTableQuery.IpIfStatsTableRequest(deviceConfiguration));
        icmpStatsQueryTask = new QueryTask<>();
        icmpStatsQueryTask.executeOnExecutor(tpe, new IcmpStatsTableQuery.IcmpStatsTableRequest(deviceConfiguration));
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

        addTableSection(context.getString(R.string.hw_info_task_view_label_table_la), laTableQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_ipsystemstats), ipSystemStatsQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_ipifstats), ipIfStatsQueryTask, queryView);
        addTableSection(context.getString(R.string.hw_info_task_view_label_table_icmpstats), icmpStatsQueryTask, queryView);
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
        laTableQueryTask.cancel(true);
        ipSystemStatsQueryTask.cancel(true);
        ipIfStatsQueryTask.cancel(true);
        icmpStatsQueryTask.cancel(true);
    }
}