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

package org.emschu.snmp.cockpit.snmp;

import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent;
import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;
import org.emschu.snmp.cockpit.query.view.AbstractCockpitQuerySection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * this class represents a currently managed device by the app
 */
public class ManagedDevice {
    private final String id;
    private final DeviceMonitorItemContent.DeviceMonitorItem fragmentListItem;
    private final DeviceConfiguration deviceConfiguration;
    private final SystemQuery initialSystemQuery;
    private final boolean isDummy;
    private SystemQuery lastSystemQuery = null;
    // tab contents
    private final ConcurrentHashMap<String, Map<Integer, AbstractCockpitQuerySection>> tabQueryCollections = new ConcurrentHashMap<>();

    /**
     * constructor
     *
     * @param id
     * @param fragmentListItem
     * @param deviceConfiguration
     * @param initialSystemQuery
     */
    public ManagedDevice(String id, DeviceMonitorItemContent.DeviceMonitorItem fragmentListItem,
                         DeviceConfiguration deviceConfiguration, SystemQuery initialSystemQuery, boolean isDummy) {
        this.id = id;
        this.fragmentListItem = fragmentListItem;
        this.deviceConfiguration = deviceConfiguration;
        this.initialSystemQuery = initialSystemQuery;
        this.isDummy = isDummy;
    }

    public String getId() {
        return id;
    }

    public boolean isDummy() {
        return isDummy;
    }

    public DeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }

    /**
     * control label in views
     *
     * @return
     */
    public String getDeviceLabel() {
        String o = initialSystemQuery.getSysName() + " | ";
        if (!deviceConfiguration.isIpv6()) {
            o += deviceConfiguration.getTargetIp()
                    + ":" + deviceConfiguration.getTargetPort();
        } else {
            o += "[" + deviceConfiguration.getTargetIp()
                    + "]:" + deviceConfiguration.getTargetPort();
        }
        return o;
    }

    /**
     * used in spinner
     *
     * @return
     */
    public String getShortDeviceLabel() {
        if (initialSystemQuery.getSysName() == null || initialSystemQuery.getSysName().length() > 32) {
            if (deviceConfiguration.isIpv6()) {
                return "[" + deviceConfiguration.getTargetIp() + "]:" + deviceConfiguration.getTargetPort();
            }
            return deviceConfiguration.getTargetIp() + ":" + deviceConfiguration.getTargetPort();
        }
        return initialSystemQuery.getSysName();
    }

    public SystemQuery getInitialSystemQuery() {
        return initialSystemQuery;
    }

    /**
     * returns last system query or initial one which should be always present
     * @return
     */
    public SystemQuery getLastSystemQuery() {
        if (lastSystemQuery != null) {
            return lastSystemQuery;
        }
        return initialSystemQuery;
    }

    @NotNull
    @Override
    public String toString() {
        return "ManagedDevice{" +
                "id='" + id + '\'' +
                ", fragmentListItem=" + fragmentListItem +
                ", deviceConfiguration=" + deviceConfiguration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedDevice that = (ManagedDevice) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void updateSystemQuery(SystemQuery systemQuery) {
        this.lastSystemQuery = systemQuery;
    }

    public Map<Integer, AbstractCockpitQuerySection> getSingleTabQueryCollection(final String idx) {
        if (!this.tabQueryCollections.containsKey(idx)) {
            this.tabQueryCollections.put(idx, new HashMap<>());
        }
        return this.tabQueryCollections.get(idx);
    }

    public ConcurrentHashMap<String, Map<Integer, AbstractCockpitQuerySection>> getTabQueryCollections() {
        return tabQueryCollections;
    }
}
