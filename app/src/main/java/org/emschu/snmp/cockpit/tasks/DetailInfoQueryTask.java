package org.emschu.snmp.cockpit.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.SimpleSnmpListRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
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
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;

import java.util.List;

/**
 * detail info tab
 */
public class DetailInfoQueryTask extends AbstractWorker {

    public static final String DETAIL_INFO_QUERY_TASK = "detail_info_query_task";

    /**
     * constructor
     *
     * @param context
     * @param workerParams
     */
    public DetailInfoQueryTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public AbstractWorkerQueryTask getTask() {
        return new Task(getManagedDevice());
    }

    class Task extends AbstractWorkerQueryTask {
        public Task(ManagedDevice managedDevice) {
            super(managedDevice);

            List<AbstractQueryRequest<? extends SnmpQuery>> queryList = this.getQueryList();
            DeviceConfiguration deviceConfiguration = getManagedDevice().getDeviceConfiguration();
            if (deviceConfiguration == null) {
                return;
            }
            queryList.add(new NetInterfaceTableQuery.NetInterfaceTableRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_iftable));
            queryList.add(new IpDefaultRouterTableQuery.IpDefaultRouterTableQueryRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_standard_router));
            queryList.add(new IpAddressTableQuery.IpAddrTableRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_ipaddr));
            queryList.add(new IpRouteTableQuery.IpRouteTableRequest(deviceConfiguration, R.string.detail_info_task_view_label_iproutetable));
            queryList.add(new IpNetToMediaTableQuery.IpNetToMediaTableRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_ipnettomedia));
            queryList.add(new AtTableQuery.AtTableRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_attable));
            queryList.add(new TcpConnectionTableQuery.TcpConnectionTableQueryRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_tcpconntable));
            queryList.add(new UdpTableQuery.UdpTableQueryRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_udpTable));
            queryList.add(new EgpNeighTableQuery.EgpNeighTableQueryRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_egpneigh));
            queryList.add(new DefaultListQuery.IpSectionQueryRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_ip_configuration));

            queryList.add(new SimpleSnmpListRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_icmp_configuration, "1.3.6.1.2.1.5"));
            queryList.add(new SimpleSnmpListRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_tcp_configuration, "1.3.6.1.2.1.6"));
            queryList.add(new SimpleSnmpListRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_udp_configuration, "1.3.6.1.2.1.7"));
            queryList.add(new SimpleSnmpListRequest(deviceConfiguration, R.string.detail_info_task_view_label_table_egp_configuration, "1.3.6.1.2.1.8"));
        }

        @Override
        public String getTabId() {
            return DETAIL_INFO_QUERY_TASK;
        }
    }
}
