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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.fragment.SingleQueryResultActivityFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * view pager for tabs of single device view
 */
public class ViewPager2Adapter extends FragmentStateAdapter {
    public static final String TAG = ViewPager2Adapter.class.getName();

    private final List<Fragment> mFragmentList = Collections.synchronizedList(new ArrayList<>());
    private final List<String> mFragmentTitleList = Collections.synchronizedList(new ArrayList<>());

    public ViewPager2Adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public ViewPager2Adapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public ViewPager2Adapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position < 0 || position >= this.mFragmentList.size()) {
            // this should never happen
            return new Fragment();
        }
        return this.mFragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return this.mFragmentList.size();
    }

    /**
     * helper method
     *
     * @param fragment
     * @param deviceId
     * @param title
     */
    public void addFragment(Fragment fragment, String deviceId, String title) {
        Bundle bundleArgs = new Bundle();
        bundleArgs.putString(TabbedDeviceActivity.EXTRA_DEVICE_ID, deviceId);

        fragment.setArguments(bundleArgs);

        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }

    public void addUserTabFragment(String deviceId, String oidQuery, String title) {
        SingleQueryResultActivityFragment singleQueryResultActivityFragment =
                new SingleQueryResultActivityFragment();
        Bundle bundleArgs = new Bundle();
        bundleArgs.putString(TabbedDeviceActivity.EXTRA_DEVICE_ID, deviceId);
        bundleArgs.putString(SingleQueryResultActivityFragment.OID_QUERY, oidQuery);
        bundleArgs.putBoolean(SingleQueryResultActivityFragment.TAB_MODE, true);

        singleQueryResultActivityFragment.setArguments(bundleArgs);

        mFragmentList.add(singleQueryResultActivityFragment);
        mFragmentTitleList.add(title);
    }

    public void clear() {
        mFragmentList.clear();
        mFragmentTitleList.clear();
        notifyDataSetChanged();
    }

    public String getTabTitle(int position) {
        if (position < 0 || position >= this.mFragmentTitleList.size()) {
            // fallback
            return "Unknown #" + position;
        }
        return this.mFragmentTitleList.get(position);
    }
}