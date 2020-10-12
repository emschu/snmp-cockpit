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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.fragment.SingleQueryResultActivityFragment;
import org.jetbrains.annotations.NotNull;

/**
 * view pager for tabs of single device view
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    public static final String TAG = ViewPagerAdapter.class.getName();

    private final List<Fragment> mFragmentList = Collections.synchronizedList(new ArrayList<>());
    private final List<String> mFragmentTitleList = Collections.synchronizedList(new ArrayList<>());

    public ViewPagerAdapter(FragmentManager manager) {
        super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
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

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

    public void clear() {
        mFragmentList.clear();
        mFragmentTitleList.clear();
        notifyDataSetChanged();
    }
}