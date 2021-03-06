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

package org.emschu.snmp.cockpit.fragment.tabs;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.fragment.DeviceFragment;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.emschu.snmp.cockpit.tasks.MonitoringQueryTask;

/**
 * fragment for hardware info
 */
public class MonitorQueryFragment extends DeviceFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_device_custom_queries, container, false);
        TextView oidDumpView = rootView.findViewById(R.id.device_detail_custom_hint);
        oidDumpView.setText(R.string.fragment_title_monitoring_info);
        // we add query sections to cockpitQueryView
        initQueryView(rootView.findViewById(R.id.default_cockpit_query_view_custom));
        return rootView;
    }

    /**
     * method to update cockpit query view
     */
    private void updateCockpitQueryView() {
        if (getQueryView() == null) {
            Log.w(TAG, "null query view");
            return;
        }
        getQueryView().clear();

        ManagedDevice md = getManagedDevice();
        if (!md.isDummy()) {
            MonitoringQueryTask backgroundTask = new MonitoringQueryTask(getQueryView(), md.getDeviceConfiguration());
            backgroundTask.executeOnExecutor(SnmpManager.getInstance().getThreadPoolExecutor());

            waitForTaskResultAsync(backgroundTask, md.getDeviceConfiguration());
        }
    }

    @Override
    public void reloadData() {
        updateCockpitQueryView();
    }
}