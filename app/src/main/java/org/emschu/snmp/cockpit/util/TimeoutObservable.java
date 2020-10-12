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

package org.emschu.snmp.cockpit.util;

import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;

/**
 * class to observe timeout events of single devices
 */
public class TimeoutObservable extends BooleanObservable {
    private final DeviceConfiguration deviceConfiguration;

    /**
     * constructor
     *
     * @param initialState
     */
    public TimeoutObservable(boolean initialState, DeviceConfiguration deviceConfiguration) {
        super(initialState);
        this.deviceConfiguration = deviceConfiguration;
    }

    public DeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }
}
