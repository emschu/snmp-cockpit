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

package org.emschu.snmp.cockpit.ui.sources

import androidx.lifecycle.MutableLiveData
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.model.QueryRegisterListItem
import org.emschu.snmp.cockpit.model.QueryRegisterTableItem
import org.emschu.snmp.cockpit.model.QueryRegisterTitledListItem
import org.emschu.snmp.cockpit.model.RegisteredQuery
import org.emschu.snmp.cockpit.snmp.query.ListQuery
import org.emschu.snmp.cockpit.snmp.query.impl.bsd.SensorTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.AtTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.EgpNeighTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.HrDeviceTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.HrDiskStorageTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.HrPartitionTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IcmpStatsTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpAddressTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpDefaultRouterTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpIfStatsTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpNetToMediaTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpRouteTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpSectionQuery
import org.emschu.snmp.cockpit.snmp.query.impl.general.IpSystemStatsTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.NetInterfaceTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.TcpConnectionTable
import org.emschu.snmp.cockpit.snmp.query.impl.general.UdpConnectionTable
import org.emschu.snmp.cockpit.snmp.query.impl.ucdavis.DskTable
import org.emschu.snmp.cockpit.snmp.query.impl.ucdavis.LaTable
import org.emschu.snmp.cockpit.ui.screens.DeviceTabItem
import org.emschu.snmp.cockpit.ui.viewmodel.QueryResponseCollection
import org.snmp4j.smi.OID

object SnmpResponseSource {
    @JvmStatic
    val responseDataGeneralTab: MutableLiveData<QueryResponseCollection> = MutableLiveData(emptyList())

    @JvmStatic
    val responseDataHardwareTab: MutableLiveData<QueryResponseCollection> = MutableLiveData(emptyList())

    @JvmStatic
    val responseDataStatusTab: MutableLiveData<QueryResponseCollection> = MutableLiveData(emptyList())

    @JvmStatic
    val responseDataCustomQueriesTab: MutableLiveData<QueryResponseCollection> = MutableLiveData(emptyList())

    @JvmStatic
    val singleQueryData: MutableLiveData<QueryResponseCollection> = MutableLiveData(emptyList())

    private val customQueryRepository = CustomQuerySource

    fun getQueriesByTab(tabScreen: DeviceTabItem): List<RegisteredQuery> =
        when (tabScreen) {
            is DeviceTabItem.General -> getDeviceTabQueryList()
            is DeviceTabItem.Hardware -> getHardwareTabQueryList()
            is DeviceTabItem.Status -> getStatusTabQueryList()
            is DeviceTabItem.Queries -> getCustomQueriesQueryList()
        }

    private fun getDeviceTabQueryList(): List<RegisteredQuery> = listOf(
        QueryRegisterTableItem(NetInterfaceTable(), R.string.detail_info_task_view_label_table_iftable),
        QueryRegisterTableItem(
            IpDefaultRouterTable(), R.string.detail_info_task_view_label_table_standard_router
        ),
        QueryRegisterTableItem(IpAddressTable(), R.string.detail_info_task_view_label_table_ipaddr),
        QueryRegisterTableItem(IpRouteTable(), R.string.detail_info_task_view_label_iproutetable),
        QueryRegisterTableItem(IpNetToMediaTable(), R.string.detail_info_task_view_label_table_ipnettomedia),
        QueryRegisterTableItem(AtTable(), R.string.detail_info_task_view_label_table_attable),
        QueryRegisterTableItem(TcpConnectionTable(), R.string.detail_info_task_view_label_table_tcpconntable),
        QueryRegisterTableItem(UdpConnectionTable(), R.string.detail_info_task_view_label_table_udpTable),
        QueryRegisterListItem(IpSectionQuery(), R.string.detail_info_task_view_label_table_ip_configuration),
        QueryRegisterListItem(
            ListQuery(OID("1.3.6.1.2.1.5")), R.string.detail_info_task_view_label_table_icmp_configuration
        ),
        QueryRegisterListItem(
            ListQuery(OID("1.3.6.1.2.1.6")), R.string.detail_info_task_view_label_table_tcp_configuration
        ),
        QueryRegisterListItem(
            ListQuery(OID("1.3.6.1.2.1.7")), R.string.detail_info_task_view_label_table_udp_configuration
        ),
        QueryRegisterListItem(
            ListQuery(OID("1.3.6.1.2.1.8")), R.string.detail_info_task_view_label_table_egp_configuration
        ),
        QueryRegisterTableItem(EgpNeighTable(), R.string.detail_info_task_view_label_table_egpneigh),
    )

    private fun getHardwareTabQueryList(): List<RegisteredQuery> = listOf(
        QueryRegisterTableItem(SensorTable(), R.string.hw_info_task_view_label_table_sensortable),
        QueryRegisterTableItem(DskTable(), R.string.hw_info_task_view_label_table_dsktable),
        QueryRegisterTableItem(HrDeviceTable(), R.string.hw_info_task_view_label_table_hrdevicetable),
        QueryRegisterTableItem(HrDiskStorageTable(), R.string.hw_info_task_view_label_table_hrdiskstorage),
        QueryRegisterTableItem(HrPartitionTable(), R.string.hw_info_task_view_label_table_hrpartitiontable),
    )

    private fun getStatusTabQueryList(): List<RegisteredQuery> = listOf(
        QueryRegisterTableItem(LaTable(), R.string.hw_info_task_view_label_table_la),
        QueryRegisterTableItem(IpSystemStatsTable(), R.string.hw_info_task_view_label_table_ipsystemstats),
        QueryRegisterTableItem(IpIfStatsTable(), R.string.hw_info_task_view_label_table_ipifstats),
        QueryRegisterTableItem(IcmpStatsTable(), R.string.hw_info_task_view_label_table_icmpstats),
    )

    private fun getCustomQueriesQueryList(): List<RegisteredQuery> {
        return (customQueryRepository.tabCustomQueries.value ?: emptyList()).map {
            QueryRegisterTitledListItem(
                ListQuery(OID(it.oid)), if (it.name == it.oid) {
                    it.name
                } else {
                    "${it.name} | ${it.oid}"
                }
            )
        }
    }
}