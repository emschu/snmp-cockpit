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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.persistence.model.CustomQuery;
import org.emschu.snmp.cockpit.query.SimpleSnmpListRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.impl.DefaultListQuery;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * this task class handles display of custom queries
 */
public class CustomQueryTask extends AsyncTask<Void, Void, Void> implements TabTaskHelper {

    public static final String TAG = CustomQueryTask.class.getName();

    private final AtomicReference<CockpitQueryView> queryView = new AtomicReference<>();
    private DeviceConfiguration deviceConfiguration = null;
    private CockpitDbHelper cockpitDbHelper = null;

    private final List<TaskWrapper> taskList = Collections.synchronizedList(new ArrayList<>());

    /**
     * constructor
     *
     * @param queryView
     * @param deviceConfiguration
     * @param dbHelper
     */
    public CustomQueryTask(CockpitQueryView queryView, DeviceConfiguration deviceConfiguration, CockpitDbHelper dbHelper) {
        this.queryView.set(queryView);
        this.deviceConfiguration = deviceConfiguration;
        this.cockpitDbHelper = dbHelper;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        ThreadPoolExecutor tpe = SnmpManager.getInstance().getThreadPoolExecutor();
        if (tpe.isTerminating() || tpe.isShutdown() || tpe.isTerminated()) {
            Log.d(TAG, "invalid thread pool given");
            return;
        }

        int rowCount = cockpitDbHelper.getQueryRowCount();
        for (int j = 0; j < rowCount; j++) {
            CustomQuery customQuery = cockpitDbHelper.getCustomQueryByListOffset(j);
            if (customQuery != null) {
                String oidToQuery = customQuery.getOid();

                Log.d(TAG, "start query task for oid:" + oidToQuery);
                QueryTask<DefaultListQuery> queryTask = new QueryTask<>();
                SimpleSnmpListRequest request = new SimpleSnmpListRequest(deviceConfiguration, oidToQuery);
                queryTask.executeOnExecutor(tpe, request);
                taskList.add(new TaskWrapper(queryTask, customQuery));
            }
        }

        if (rowCount == 0) {
            // finish loading if user has no queries
            this.queryView.get().render(true);
        }

        cockpitDbHelper.close();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (deviceConfiguration == null) {
            throw new IllegalArgumentException("null DeviceConfiguration given");
        }

        for (TaskWrapper taskWrapper : taskList) {
            CustomQuery customQuery = taskWrapper.getCustomQuery();
            addListQuery(customQuery.getName() + " | " + customQuery.getOid(), taskWrapper.getQueryTask(), queryView);
        }

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
        for (TaskWrapper taskWrapper: taskList) {
            taskWrapper.getQueryTask().cancel(true);
        }
    }

    /**
     * simple wrapper for two objects
     */
    class TaskWrapper {
        private final QueryTask<? extends SnmpQuery> queryTask;
        private final CustomQuery customQuery;

        TaskWrapper(QueryTask<? extends SnmpQuery> queryTask, CustomQuery customQuery) {
            this.queryTask = queryTask;
            this.customQuery = customQuery;
        }

        QueryTask<? extends SnmpQuery> getQueryTask() {
            return queryTask;
        }

        CustomQuery getCustomQuery() {
            return customQuery;
        }
    }
}
