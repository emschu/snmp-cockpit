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

import org.snmp4j.smi.OID;

import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;

/**
 * general interface which needs to be implemented by each query class
 *
 * @param <T>
 */
public interface QueryRequest<T extends SnmpQuery> {
    /**
     * a {@link DeviceConfiguration} instance
     *
     * @return
     */
    public DeviceConfiguration getDeviceConfiguration();

    /**
     * this query decides if we use #querySingle or #queryWalk to retrieve results
     *
     * @return
     */
    public boolean isSingleRequest();

    /**
     * the oid which is requested
     *
     * @return
     */
    public OID getOidQuery();

    /**
     * the concrete {@link SnmpQuery} implementation class
     * TODO use already defined generic instead of this method
     *
     * @return
     */
    public Class<T> getQueryClass();


    /**
     * indicates wheter this query should be kept in cache or not
     *
     * @return
     */
    public default boolean isCacheable() {
        return true;
    }

    /**
     * used to identify this query in a cache
     *
     * @return
     */
    public default String getCacheId() {
        return this.getClass().getSimpleName() + getDeviceConfiguration().getUniqueDeviceId();
    }
}
