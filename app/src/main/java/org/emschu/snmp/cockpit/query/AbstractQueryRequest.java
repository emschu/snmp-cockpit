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

package org.emschu.snmp.cockpit.query;

import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;

/**
 * abstract class for each query request
 * the subclasses are more interesting
 *
 * @param <T>
 */
public abstract class AbstractQueryRequest<T extends SnmpQuery> implements QueryRequest<T> {
    private final DeviceConfiguration deviceConfiguration;
    private final int contentTitleResourceId;

    public AbstractQueryRequest(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
        this.contentTitleResourceId = 0;
    }

    public AbstractQueryRequest(DeviceConfiguration deviceConfiguration, int contentTitleResourceId) {
        this.deviceConfiguration = deviceConfiguration;
        this.contentTitleResourceId = contentTitleResourceId;
    }

    public DeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }

    public int getContentTitleResourceId() {
        return contentTitleResourceId;
    }
}
