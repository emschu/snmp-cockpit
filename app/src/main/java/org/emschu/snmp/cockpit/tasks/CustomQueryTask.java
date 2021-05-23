package org.emschu.snmp.cockpit.tasks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.persistence.model.CustomQuery;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.impl.CustomListQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;

import java.util.List;

public class CustomQueryTask extends AbstractWorker {
    public static final String CUSTOM_QUERY_TASK = "custom_query_task";
    private final CockpitDbHelper dbHelper;

    public CustomQueryTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.dbHelper = new CockpitDbHelper(context);
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

            int rowCount = dbHelper.getQueryRowCount();
            for (int j = 0; j < rowCount; j++) {
                CustomQuery customQuery = dbHelper.getCustomQueryByListOffset(j);
                if (customQuery != null) {
                    String oidToQuery = customQuery.getOid();
                    Log.d(TAG, "start query task for oid:" + oidToQuery);

                    queryList.add(new CustomListQuery.CustomQueryRequest(deviceConfiguration, customQuery));
                }
            }
        }

        @Override
        public String getTabId() {
            return CUSTOM_QUERY_TASK;
        }
    }

    @Override
    public void cleanUp() {
        super.cleanUp();

        this.dbHelper.close();
    }
}
