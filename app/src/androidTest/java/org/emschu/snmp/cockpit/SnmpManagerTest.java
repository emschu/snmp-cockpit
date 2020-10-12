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
import org.snmp4j.smi.OID;

import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpManager;


@RunWith(AndroidJUnit4.class)
public class SnmpManagerTest {

    @Rule
    public ActivityTestRule<CockpitMainActivity> rule = new ActivityTestRule<>(CockpitMainActivity.class, true, true);

    @Before
    public void setUp() throws Exception {
        rule.launchActivity(new Intent());

        rule.getActivity().stopProtection(rule.getActivity());
    }

    @Test
    public void testDeviceConfigLabelsAreImplemented() {
        DeviceConfiguration deviceConfiguration = new DeviceConfiguration();
        for (OID authProtocol : SnmpManager.getInstance().getAuthProtocols()) {
            deviceConfiguration.setAuthProtocol(authProtocol);
            Assert.assertNotNull(deviceConfiguration.getAuthProtocolLabel());
        }
        for (OID privProtocol : SnmpManager.getInstance().getPrivProtocols()) {
            deviceConfiguration.setPrivProtocol(privProtocol);
            Assert.assertNotNull(deviceConfiguration.getPrivProtocolLabel());
        }
    }
}
