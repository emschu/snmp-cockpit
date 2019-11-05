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

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.SnmpConfigurationFactory;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.COMMUNITY_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.ENC_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.HOST_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.PORT_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.USER_KEY;
import static org.emschu.snmp.cockpit.activity.SNMPLoginActivity.USER_PASSPHRASE_KEY;

@RunWith(AndroidJUnit4.class)
public class DeviceHandlingTest {

    @Rule
    public ActivityTestRule<CockpitMainActivity> rule = new ActivityTestRule<>(CockpitMainActivity.class, true, true);

    @Before
    public void setUp() throws Exception {
        rule.launchActivity(new Intent());
    }

    @Test
    public void testAddDeviceAndRemove() {
        List<DeviceMonitorItemContent.DeviceMonitorItem> deviceList = DeviceManager.getInstance().getDeviceList();
        Assert.assertEquals(0, deviceList.size());
        DeviceManager.getInstance().add(new SnmpConfigurationFactory().createDummyV1Config(
                "192.168.150.1",
                "public"
        ), true);
        Assert.assertEquals(1, deviceList.size());

        String testDeviceId = deviceList.get(0).getDeviceConfiguration().getUniqueDeviceId();
        DeviceManager.getInstance().removeItem(testDeviceId);
        Assert.assertEquals(0, deviceList.size());
        Assert.assertFalse(SnmpManager.getInstance().doesConnectionExist(testDeviceId));
    }

    @Test
    public void testAddAllDeviceRemoval() {
        List<DeviceMonitorItemContent.DeviceMonitorItem> deviceList = DeviceManager.getInstance().getDeviceList();
        Assert.assertEquals(0, deviceList.size());
        Assert.assertFalse(SnmpManager.getInstance().doesConnectionExist("test"));
        DeviceManager.getInstance().add(new SnmpConfigurationFactory().createDummyV1Config(
                "192.168.150.1",
                "public"
        ), true);
        Assert.assertEquals(1, deviceList.size());
        String testDeviceId = deviceList.get(0).getDeviceConfiguration().getUniqueDeviceId();
        DeviceManager.getInstance().removeAllItems();
        Assert.assertEquals(0, deviceList.size());
        Assert.assertFalse(SnmpManager.getInstance().doesConnectionExist(testDeviceId));
    }

    @Test
    public void testSystemQueryIsUpdateableOnDummyItem() {
        List<DeviceMonitorItemContent.DeviceMonitorItem> deviceList = DeviceManager.getInstance().getDeviceList();
        Assert.assertEquals(0, deviceList.size());
        Assert.assertFalse(SnmpManager.getInstance().doesConnectionExist("test"));
        DeviceManager.getInstance().add(new SnmpConfigurationFactory().createDummyV1Config(
                "192.168.150.1",
                "public"
        ), true);
        Assert.assertEquals(1, deviceList.size());
        DeviceManager.getInstance().updateSystemQueries();
        DeviceManager.getInstance().removeAllItems();
    }

    @Test
    public void testDeviceManagerIntentProcessing() {
        DeviceManager deviceManager = DeviceManager.getInstance();
        Intent intent = getV1Intent();
        DeviceConfiguration configurationv1 = deviceManager.createDeviceConfiguration(intent);
        Assert.assertEquals(DeviceConfiguration.SNMP_VERSION.v1, configurationv1.getSnmpVersionEnum());
        Assert.assertEquals(intent.getStringExtra(HOST_KEY), configurationv1.getTargetIp());
        Assert.assertEquals(intent.getStringExtra(PORT_KEY), "" + configurationv1.getTargetPort());
        Assert.assertEquals(intent.getStringExtra(COMMUNITY_KEY), configurationv1.getUsername());

        Intent intentv3 = getV3Intent();
        DeviceConfiguration configurationv3 = deviceManager.createDeviceConfiguration(intentv3);
        Assert.assertEquals(DeviceConfiguration.SNMP_VERSION.v3, configurationv3.getSnmpVersionEnum());
        Assert.assertEquals(intentv3.getStringExtra(HOST_KEY), configurationv3.getTargetIp());
        Assert.assertEquals(intentv3.getStringExtra(PORT_KEY), "" + configurationv3.getTargetPort());
        Assert.assertEquals(intentv3.getStringExtra(USER_KEY), configurationv3.getUsername());
        Assert.assertEquals(intentv3.getStringExtra(USER_PASSPHRASE_KEY), configurationv3.getAuthPassphrase());
        Assert.assertEquals(intentv3.getStringExtra(ENC_KEY), configurationv3.getPrivacyPassphrase());
    }

    @NonNull
    public Intent getV1Intent() {
        Intent intent = new Intent();
        String testHost = "192.168.143.178";
        intent.putExtra(HOST_KEY, testHost);
        intent.putExtra(PORT_KEY, "166");
        intent.putExtra(COMMUNITY_KEY, "public");
        intent.putExtra(USER_KEY, "");
        intent.putExtra(USER_PASSPHRASE_KEY, "");
        intent.putExtra(ENC_KEY, "");
        return intent;
    }


    @NonNull
    public Intent getV3Intent() {
        Intent intent = new Intent();
        String testHost = "192.168.143.179";
        intent.putExtra(HOST_KEY, testHost);
        intent.putExtra(PORT_KEY, "165");
        intent.putExtra(COMMUNITY_KEY, "");
        intent.putExtra(USER_KEY, "user");
        intent.putExtra(USER_PASSPHRASE_KEY, "pass");
        intent.putExtra(ENC_KEY, "enc");
        return intent;
    }

}
