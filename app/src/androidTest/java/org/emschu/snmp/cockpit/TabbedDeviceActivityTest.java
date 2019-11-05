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

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.SnmpConfigurationFactory;

import static androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread;

@RunWith(AndroidJUnit4.class)
public class TabbedDeviceActivityTest {

    @Rule
    public ActivityTestRule<TabbedDeviceActivity> rule = new ActivityTestRule<>(TabbedDeviceActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        DeviceConfiguration dummyConfig = new SnmpConfigurationFactory().createDummyV1Config("192.165.213.123", "public");
        DeviceManager.getInstance().add(dummyConfig, true);
        Assert.assertNotNull(dummyConfig);
        Assert.assertNotNull(dummyConfig.getUniqueConnectionId());
        Intent startIntent = new Intent();
        startIntent.putExtra(TabbedDeviceActivity.EXTRA_DEVICE_ID, dummyConfig.getUniqueDeviceId());
        rule.launchActivity(startIntent);
    }

    @Test
    public void testRefreshWorksOnDummy() throws Throwable {
        runOnUiThread(() -> {
            rule.getActivity().refreshView(true);
            rule.getActivity().restartQueryCall();
            Assert.assertTrue(true);
        });
    }
}
