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

package org.emschu.snmp.cockpit.snmp.query.impl.general

import org.emschu.snmp.cockpit.snmp.QueryResponse
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.OID

data class SystemQuery(
    var sysName: String = "",
    var sysDescr: String = "",
    var sysLocation: String = "",
    var sysContact: String = "",
    var sysObjectId: String = "",
    var sysUpTime: String = "",
    var sysServices: String = "",
) : org.emschu.snmp.cockpit.snmp.query.ListQuery(OID(intArrayOf(1, 3, 6, 1, 2, 1, 1))) {
    override fun processResult(queryResponses: Collection<QueryResponse>) {
        super.processResult(queryResponses)
        val resultList = queryResponses.toList()

        sysDescr = getOIDValue(resultList, SnmpConstants.sysDescr)
        sysObjectId = getOIDValue(resultList, SnmpConstants.sysObjectID)
        sysUpTime = getOIDValue(resultList, SnmpConstants.sysUpTime)
        sysContact = getOIDValue(resultList, SnmpConstants.sysContact)
        sysName = getOIDValue(resultList, SnmpConstants.sysName)
        sysLocation = getOIDValue(resultList, SnmpConstants.sysLocation)
        sysServices = getOIDValue(resultList, SnmpConstants.sysServices)
    }
}