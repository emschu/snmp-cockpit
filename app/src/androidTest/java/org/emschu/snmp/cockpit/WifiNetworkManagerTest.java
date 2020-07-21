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

package org.emschu.snmp.cockpit;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.emschu.snmp.cockpit.network.WifiNetworkManager;

@RunWith(AndroidJUnit4.class)
public class WifiNetworkManagerTest {

    @Rule
    public ActivityTestRule<CockpitMainActivity> rule = new ActivityTestRule<>(CockpitMainActivity.class);

    @Test
    public void testWifiModesAndPreferenceInteraction() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(rule.getActivity());
        SharedPreferences.Editor edit = sharedPreferences.edit();

        edit.putBoolean(CockpitPreferenceManager.KEY_IS_WIFI_SSID_LOCKED, false);
        edit.putBoolean(CockpitPreferenceManager.KEY_IS_WPA2_ONLY, false);
        edit.putBoolean(CockpitPreferenceManager.KEY_DEBUG_ALLOW_ALL_NETWORKS, false);
        edit.apply();

        WifiNetworkManager wifiNetworkManager = WifiNetworkManager.getInstance();
        wifiNetworkManager.updateMode();
        Assert.assertEquals("AndroidWifi", wifiNetworkManager.getCurrentSsid());
        Assert.assertEquals(4, wifiNetworkManager.getCurrentMode());

        edit.putBoolean(CockpitPreferenceManager.KEY_IS_WPA2_ONLY, true);
        edit.apply();
        wifiNetworkManager.updateMode();
        Assert.assertEquals(1, wifiNetworkManager.getCurrentMode());

        edit.putBoolean(CockpitPreferenceManager.KEY_IS_WIFI_SSID_LOCKED, true);
        edit.apply();
        wifiNetworkManager.updateMode();
        Assert.assertEquals(3, wifiNetworkManager.getCurrentMode());

        edit.putBoolean(CockpitPreferenceManager.KEY_IS_WPA2_ONLY, false);
        edit.apply();
        wifiNetworkManager.updateMode();
        Assert.assertEquals(2, wifiNetworkManager.getCurrentMode());
    }
}
