/*
 * snmp-cockpit
 *
 * Copyright (C) 2018-2023
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.emschu.snmp.cockpit

import androidx.lifecycle.MutableLiveData
import org.emschu.snmp.cockpit.snmp.query.QueryCache
import org.snmp4j.mp.MPv3
import org.snmp4j.smi.OctetString

/**
 * activities are coming and going - this class should last to hold the (network security) state of the app
 */
object CockpitStateManager {
    // observables
    val networkAvailabilityObservable = MutableLiveData(true)

    /**
     * get app wide unique local engine id
     *
     * @return
     */
    val localEngineId: OctetString by lazy { OctetString(MPv3.createLocalEngineID()) }
    val queryCache = QueryCache()
}