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

package org.emschu.snmp.cockpit.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.query.SimpleSnmpListRequest;
import org.emschu.snmp.cockpit.query.impl.DefaultListQuery;
import org.emschu.snmp.cockpit.query.impl.general.AtTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.EgpNeighTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpAddressTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpDefaultRouterTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpNetToMediaTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpRouteTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.NetInterfaceTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.TcpConnectionTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.UdpTableQuery;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * this class is called when detail info of a device is requested
 */
public class DetailInfoQueryTask extends AsyncTask<Void, Void, Void> implements TabTaskHelper {

    public static final String TAG = DetailInfoQueryTask.class.getName();

    private final AtomicReference<CockpitQueryView> queryView = new AtomicReference<>();
    private final DeviceConfiguration deviceConfiguration;

    // start and send queries
    private final QueryTask<NetInterfaceTableQuery> netInterfaceTableQueryTask = new QueryTask<>();
    private final QueryTask<IpAddressTableQuery> ipQueryTask = new QueryTask<>();
    private final QueryTask<IpRouteTableQuery> ipRouteTableTask = new QueryTask<>();
    private final QueryTask<IpNetToMediaTableQuery> ipNetToMediaTask = new QueryTask<>();
    private final QueryTask<AtTableQuery> atTask = new QueryTask<>();
    private final QueryTask<TcpConnectionTableQuery> tcpConnectionTask = new QueryTask<>();
    private final QueryTask<UdpTableQuery> udpConnectionTask = new QueryTask<>();
    private final QueryTask<EgpNeighTableQuery> egpNeighTableQuery = new QueryTask<>();
    private final QueryTask<IpDefaultRouterTableQuery> ipDefaultRouterQueryTask = new QueryTask<>();
    private final QueryTask<DefaultListQuery> ipInfoTask = new QueryTask<>();
    private final QueryTask<DefaultListQuery> icmpInfoTask = new QueryTask<>();
    private final QueryTask<DefaultListQuery> tcpInfoTask = new QueryTask<>();
    private final QueryTask<DefaultListQuery> udpInfoTask = new QueryTask<>();
    private final QueryTask<DefaultListQuery> egpInfoTask = new QueryTask<>();

    /**
     * constructor
     *
     * @param queryView
     * @param deviceConfiguration
     */
    public DetailInfoQueryTask(CockpitQueryView queryView, DeviceConfiguration deviceConfiguration) {
        this.queryView.set(queryView);
        this.deviceConfiguration = deviceConfiguration;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "entering detail info query task");

        super.onPreExecute();

        ThreadPoolExecutor tpe = SnmpManager.getInstance().getThreadPoolExecutor();
        if (tpe.isTerminating() || tpe.isShutdown() || tpe.isTerminated()) {
            Log.d(TAG, "invalid thread pool given");
            return;
        }
        netInterfaceTableQueryTask.executeOnExecutor(tpe, new NetInterfaceTableQuery.NetInterfaceTableRequest(deviceConfiguration));
        ipDefaultRouterQueryTask.executeOnExecutor(tpe, new IpDefaultRouterTableQuery.IpDefaultRouterTableQueryRequest(deviceConfiguration));
        ipQueryTask.executeOnExecutor(tpe, new IpAddressTableQuery.IpAddrTableRequest(deviceConfiguration));
        ipRouteTableTask.executeOnExecutor(tpe, new IpRouteTableQuery.IpRouteTableRequest(deviceConfiguration));
        ipNetToMediaTask.executeOnExecutor(tpe, new IpNetToMediaTableQuery.IpNetToMediaTableRequest(deviceConfiguration));
        atTask.executeOnExecutor(tpe, new AtTableQuery.AtTableRequest(deviceConfiguration));
        tcpConnectionTask.executeOnExecutor(tpe, new TcpConnectionTableQuery.TcpConnectionTableQueryRequest(deviceConfiguration));
        udpConnectionTask.executeOnExecutor(tpe, new UdpTableQuery.UdpTableQueryRequest(deviceConfiguration));
        egpNeighTableQuery.executeOnExecutor(tpe, new EgpNeighTableQuery.EgpNeighTableQueryRequest(deviceConfiguration));
        ipInfoTask.executeOnExecutor(tpe, new DefaultListQuery.IpSectionQueryRequest(deviceConfiguration));
        icmpInfoTask.executeOnExecutor(tpe, new SimpleSnmpListRequest(deviceConfiguration, "1.3.6.1.2.1.5"));
        tcpInfoTask.executeOnExecutor(tpe, new SimpleSnmpListRequest(deviceConfiguration, "1.3.6.1.2.1.6"));
        udpInfoTask.executeOnExecutor(tpe, new SimpleSnmpListRequest(deviceConfiguration, "1.3.6.1.2.1.7"));
        egpInfoTask.executeOnExecutor(tpe, new SimpleSnmpListRequest(deviceConfiguration, "1.3.6.1.2.1.8"));
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (deviceConfiguration == null) {
            throw new IllegalArgumentException("null DeviceConfiguration given");
        }
        Context context = queryView.get().getContext();
        if (context == null) {
            throw new IllegalArgumentException("null context given");
        }
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_iftable), netInterfaceTableQueryTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_standard_router), ipDefaultRouterQueryTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_ipaddr), ipQueryTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_iproutetable), ipRouteTableTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_ipnettomedia), ipNetToMediaTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_attable), atTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_tcpconntable), tcpConnectionTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_udpTable), udpConnectionTask, queryView);
        addTableSection(context.getString(R.string.detail_info_task_view_label_table_egpneigh), egpNeighTableQuery, queryView);

        // add list query
        addListQuery(context.getString(R.string.detail_info_task_view_label_table_ip_configuration), ipInfoTask, queryView);
        addListQuery(context.getString(R.string.detail_info_task_view_label_table_icmp_configuration), icmpInfoTask, queryView);
        addListQuery(context.getString(R.string.detail_info_task_view_label_table_tcp_configuration), tcpInfoTask, queryView);
        addListQuery(context.getString(R.string.detail_info_task_view_label_table_udp_configuration), udpInfoTask, queryView);
        addListQuery(context.getString(R.string.detail_info_task_view_label_table_egp_configuration), egpInfoTask, queryView);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG, "start rendering");
        // here we are on the ui thread
        queryView.get().render(true);
    }

    @Override
    public void cancelTasks() {
        netInterfaceTableQueryTask.cancel(true);
        ipDefaultRouterQueryTask.cancel(true);
        ipQueryTask.cancel(true);
        ipRouteTableTask.cancel(true);
        ipNetToMediaTask.cancel(true);
        atTask.cancel(true);
        tcpConnectionTask.cancel(true);
        udpConnectionTask.cancel(true);
        egpNeighTableQuery.cancel(true);
        ipInfoTask.cancel(true);
        icmpInfoTask.cancel(true);
        tcpInfoTask.cancel(true);
        udpInfoTask.cancel(true);
        egpInfoTask.cancel(true);
    }
}