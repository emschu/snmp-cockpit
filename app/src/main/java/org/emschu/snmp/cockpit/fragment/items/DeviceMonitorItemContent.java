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

package org.emschu.snmp.cockpit.fragment.items;

import java.util.List;
import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.jetbrains.annotations.NotNull;

/**
 * device monitor item content class
 */
public class DeviceMonitorItemContent {

    private DeviceMonitorItemContent() { }

    /**
     * array of sample items.
     */
    private static final List<DeviceMonitorItem> ITEMS = DeviceManager.getInstance().getDeviceList();

    public static List<DeviceMonitorItem> getItems() {
        return ITEMS;
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class DeviceMonitorItem {
        public final String id;
        public final String host;
        public final String port;
        public final DeviceConfiguration deviceConfiguration;
        public final SystemQuery systemQuery;

        /**
         * constructor
         *
         * @param id
         * @param host
         * @param port
         * @param deviceConfiguration
         * @param systemQuery
         */
        public DeviceMonitorItem(String id, String host, String port, DeviceConfiguration deviceConfiguration, SystemQuery systemQuery) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.deviceConfiguration = deviceConfiguration;
            this.systemQuery = systemQuery;
        }

        public DeviceConfiguration getDeviceConfiguration() {
            return deviceConfiguration;
        }

        @NotNull
        @Override
        public String toString() {
            return "DeviceMonitorItem{" +
                    "id='" + id + '\'' +
                    ", host='" + host + '\'' +
                    ", port='" + port + '\'' +
                    ", deviceConfiguration=" + deviceConfiguration +
                    '}';
        }
    }
}
