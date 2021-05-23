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

package org.emschu.snmp.cockpit.fragment;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.SingleQueryResultActivity;
import org.emschu.snmp.cockpit.query.SimpleSnmpListRequest;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;

/**
 * A placeholder fragment containing a simple view.
 */
public class SingleQueryResultActivityFragment extends DeviceFragment {

    public static final String TAG = SingleQueryResultActivityFragment.class.getName();
    public static final String OID_QUERY = "oid_query";
    public static final String TAB_MODE = "tab_mode";
    private CockpitQueryView cockpitQueryView;

    public SingleQueryResultActivityFragment() {
        // mandatory empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_query_result, container, false);
        cockpitQueryView = rootView.findViewById(R.id.single_cockpit_query_view);

        Pair<String, String> argsTuple = getDeviceIdOidTuple();
        String deviceId = argsTuple.first;
        String oidQuery = argsTuple.second;

        if (deviceId == null || oidQuery == null) {
            Log.e(TAG, "empty deviceId or oidQuery intent extra");
            return rootView;
        }

        ManagedDevice md = DeviceManager.getInstance().getDevice(deviceId);
        if (md == null) {
            Log.e(TAG, "no device found for deviceId: " + deviceId);
            return rootView;
        }

        cockpitQueryView.addListQuery("OID-Abfrage: " + oidQuery,
                new SimpleSnmpListRequest(md.getDeviceConfiguration(), oidQuery), true);
        return rootView;
    }

    private Pair<String, String> getDeviceIdOidTuple() {
        boolean isTabMode = false;
        if (getArguments() != null) {
            isTabMode = getArguments().getBoolean(TAB_MODE, false);
        }
        String deviceId;
        String oidQuery;
        if (getActivity() != null && !isTabMode) {
            deviceId = getActivity().getIntent().getStringExtra(SingleQueryResultActivity.EXTRA_DEVICE_ID);
            oidQuery = getActivity().getIntent().getStringExtra(SingleQueryResultActivity.EXTRA_OID_QUERY);
        } else {
            deviceId = getArguments().getString(SingleQueryResultActivity.EXTRA_DEVICE_ID);
            oidQuery = getArguments().getString(SingleQueryResultActivity.EXTRA_OID_QUERY);
        }
        return Pair.create(deviceId, oidQuery);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (cockpitQueryView != null) {
            cockpitQueryView.render(true);
        }
    }

    /**
     *
     */
    @Override
    public void reloadData() {
        if (cockpitQueryView == null) {
            Log.w(TAG, "null cockpit query view");
            return;
        }
        cockpitQueryView.clear();
        String currentOidQuery = getDeviceIdOidTuple().second;
        if (currentOidQuery == null || currentOidQuery.trim().isEmpty()) {
            Log.e(TAG, "no current oid query found");
            return;
        }
        cockpitQueryView.addListQuery("OID-Abfrage: " + currentOidQuery,
                new SimpleSnmpListRequest(getManagedDevice().getDeviceConfiguration(), currentOidQuery.trim()), true);
        cockpitQueryView.render(true);
    }
}
