package org.emschu.snmp.cockpit.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.impl.bsd.SensorTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.HrDeviceTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.HrDiskStorageTableQuery;
import org.emschu.snmp.cockpit.query.impl.general.HrPartitionTableQuery;
import org.emschu.snmp.cockpit.query.impl.ucdavis.DskTableQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HardwareQueryTask extends AbstractWorker {

    public static final String HARDWARE_QUERY_TASK = "hardware_query_task";

    public HardwareQueryTask(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
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
            queryList.add(new SensorTableQuery.SensorTableQueryRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_sensortable));
            queryList.add(new DskTableQuery.DskTableQueryRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_dsktable));
            queryList.add(new HrDeviceTableQuery.HrDeviceTableRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_hrdevicetable));
            queryList.add(new HrDiskStorageTableQuery.HrDiskStorageTableRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_hrdiskstorage));
            queryList.add(new HrPartitionTableQuery.HrPartitionTableRequest(deviceConfiguration, R.string.hw_info_task_view_label_table_hrpartitiontable));
        }

        @Override
        public String getTabId() {
            return HARDWARE_QUERY_TASK;
        }
    }
}
