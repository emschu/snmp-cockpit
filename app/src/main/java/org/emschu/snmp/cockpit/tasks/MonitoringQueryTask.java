package org.emschu.snmp.cockpit.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.impl.general.IcmpStatsTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpIfStatsTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.IpSystemStatsTableQuery;
import org.emschu.snmp.cockpit.query.impl.ucdavis.LaTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MonitoringQueryTask extends AbstractWorker {
    public static final String MONITORING_QUERY_TASK = "monitoring_query_task";

    public MonitoringQueryTask(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
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

            queryList.add(new LaTableQuery.LaTableQueryRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_la));
            queryList.add(new IpSystemStatsTableQuery.IpSystemStatsTableRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_ipsystemstats));
            queryList.add(new IpIfStatsTableQuery.IpIfStatsTableRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_ipifstats));
            queryList.add(new IcmpStatsTableQuery.IcmpStatsTableRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_icmpstats));
        }

        @Override
        public String getTabId() {
            return MONITORING_QUERY_TASK;
        }
    }
}
