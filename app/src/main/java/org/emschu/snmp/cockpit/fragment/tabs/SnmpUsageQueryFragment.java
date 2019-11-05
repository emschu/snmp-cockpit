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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.fragment.DeviceFragment;
import org.emschu.snmp.cockpit.query.impl.DefaultListQuery;
import org.emschu.snmp.cockpit.query.impl.general.SysORTableQuery;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;

/**
 * fragment for snmp usage
 */
public class SnmpUsageQueryFragment extends DeviceFragment {

    private String snmpUsageTitle = null;
    private String snmpMibsTitle = null;
    private String mibsOrTitle = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_device_usage_queries, container, false);
        TextView hintTextView = rootView.findViewById(R.id.device_detail_usage_hint);
        hintTextView.setText(R.string.please_wait_label);

        ManagedDevice md = getManagedDevice();
        if (md == null) {
            throw new IllegalStateException("no managed device found!");
        }

        initQueryView(rootView.findViewById(R.id.default_cockpit_query_view_usage));

        hintTextView.setText(R.string.device_detail_usage_tab_hint);

        getQueryView().render(true);
        snmpUsageTitle = getString(R.string.snmp_usage_tab_content_title);
        snmpMibsTitle = getString(R.string.snmp_usage_tab_content_mibs);
        mibsOrTitle = getString(R.string.snmp_usage_tab_content_mibs);

        return rootView;
    }

    @Override
    public void reloadData() {
        refresh();
    }

    public void refresh() {
        CockpitQueryView queryView = getQueryView();
        if (queryView != null) {
            queryView.clear();
            DeviceConfiguration deviceConfiguration = getManagedDevice().getDeviceConfiguration();
            queryView.addTableQuery(mibsOrTitle + " | sysORTable", new SysORTableQuery.SysORTableQueryRequest(deviceConfiguration), true);

            queryView.addListQuery(snmpMibsTitle, new DefaultListQuery.MrTableQueryRequest(deviceConfiguration));

            queryView.addListQuery(snmpUsageTitle,
                    new DefaultListQuery.SnmpUsageQueryRequest(deviceConfiguration));

            queryView.render(true);
        }
    }
}