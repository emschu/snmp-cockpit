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

package org.emschu.snmp.cockpit.adapter;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.fragment.SingleQueryResultActivityFragment;
import org.emschu.snmp.cockpit.fragment.tabs.DeviceCustomQueryFragment;
import org.emschu.snmp.cockpit.fragment.tabs.DeviceDetailFragment;
import org.emschu.snmp.cockpit.fragment.tabs.HardwareQueryFragment;
import org.emschu.snmp.cockpit.fragment.tabs.MonitorQueryFragment;
import org.emschu.snmp.cockpit.fragment.tabs.SnmpUsageQueryFragment;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.snmp.DeviceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * view pager for tabs of single device view
 */
public class ViewPager2Adapter extends FragmentStateAdapter {
    public static final String TAG = ViewPager2Adapter.class.getName();

    private String deviceId;
    private CockpitDbHelper cockpitDbHelper;
    private String openTabId;

    private final List<String> mFragmentTitleList = Collections.synchronizedList(new ArrayList<>());

    public ViewPager2Adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public ViewPager2Adapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public void initTitles(@NonNull Resources resources) {
        this.mFragmentTitleList.add(resources.getString(R.string.device_detail_fragment_tab_title));
        this.mFragmentTitleList.add(resources.getString(R.string.device_detail_tab_hardware_label));
        this.mFragmentTitleList.add(resources.getString(R.string.device_detail_snmp_usage_query_label));

        if (this.cockpitDbHelper.getQueryRowCount() > 0) {
            this.mFragmentTitleList.add(resources.getString(R.string.device_custom_query_tab_title));
        }
        this.mFragmentTitleList.add(resources.getString(R.string.snmp_usage_tab_content_title));

        List<String> oidQueryList = DeviceManager.getInstance().getTabs(deviceId);
        if (oidQueryList != null) {
            Log.d(TAG, "found: " + oidQueryList.size() + " user defined tabs");
            int i = 1;
            for (String oidQuery : oidQueryList) {
                this.mFragmentTitleList.add(i + " | " + oidQuery);
                i++;
            }
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle bundle = new Bundle();
        bundle.putString(TabbedDeviceActivity.EXTRA_DEVICE_ID, deviceId);

        switch (position) {
            case 0:
                return this.prepareFragment(new DeviceDetailFragment(), bundle);
            case 1:
                return this.prepareFragment(new HardwareQueryFragment(), bundle);
            case 2:
                return this.prepareFragment(new MonitorQueryFragment(), bundle);
            case 3:
                if (cockpitDbHelper.getQueryRowCount() > 0) {
                    return this.prepareFragment(new DeviceCustomQueryFragment(), bundle);
                }
                return this.prepareFragment(new SnmpUsageQueryFragment(), bundle);
            default:
                if (position == 4 && cockpitDbHelper.getQueryRowCount() > 0) {
                    return this.prepareFragment(new SnmpUsageQueryFragment(), bundle);
                }
                final int customIndexDifference = this.getItemCount() - position;

                List<String> oidQueryList = DeviceManager.getInstance().getTabs(deviceId);
                if (oidQueryList != null && !oidQueryList.isEmpty()) {
                    final int customIndex = oidQueryList.size() - customIndexDifference;

                    Log.d(TAG, "found: " + oidQueryList.size() + " user defined tabs");
                    String oidQuery = oidQueryList.get(customIndex);

                    return this.getUserTabFragment(deviceId, oidQuery);
                }
                throw new IllegalStateException(String.format("Cannot find a tab with position '%s'", position));
        }
    }

    private Fragment prepareFragment(@NonNull Fragment fragmentInstance, @NonNull Bundle arguments) {
        fragmentInstance.setArguments(arguments);
        return fragmentInstance;
    }

    @Override
    public int getItemCount() {
        int customQueryTab = 0;
        if (cockpitDbHelper.getQueryRowCount() > 0) {
            customQueryTab = 1;
        }
        int customOidTabs = 0;
        List<String> oidQueryList = DeviceManager.getInstance().getTabs(deviceId);
        if (oidQueryList != null && oidQueryList.size() > 0) {
            customOidTabs = oidQueryList.size();
        }
        return 4 + customQueryTab + customOidTabs;
    }

    public Fragment getUserTabFragment(String deviceId, String oidQuery) {
        SingleQueryResultActivityFragment singleQueryResultActivityFragment =
                new SingleQueryResultActivityFragment();
        Bundle bundleArgs = new Bundle();
        bundleArgs.putString(TabbedDeviceActivity.EXTRA_DEVICE_ID, deviceId);
        bundleArgs.putString(SingleQueryResultActivityFragment.OID_QUERY, oidQuery);
        bundleArgs.putBoolean(SingleQueryResultActivityFragment.TAB_MODE, true);

        singleQueryResultActivityFragment.setArguments(bundleArgs);

        return singleQueryResultActivityFragment;
    }

    public String getTabTitle(int position) {
        if (position < 0 || position >= this.mFragmentTitleList.size()) {
            // fallback
            return "Unknown #" + position;
        }
        return this.mFragmentTitleList.get(position);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public CockpitDbHelper getCockpitDbHelper() {
        return cockpitDbHelper;
    }

    public void setCockpitDbHelper(CockpitDbHelper cockpitDbHelper) {
        this.cockpitDbHelper = cockpitDbHelper;
    }

    public String getOpenTabId() {
        return openTabId;
    }

    public void setOpenTabId(String openTabId) {
        this.openTabId = openTabId;
    }
}