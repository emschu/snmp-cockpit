package org.emschu.snmp.cockpit.tasks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent;
import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshDeviceListTask extends Worker {
    private static final String TAG = RefreshDeviceListTask.class.getSimpleName();

    public RefreshDeviceListTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Data inputData = workerParams.getInputData();
        System.out.println(inputData);
    }

    @NonNull
    @Override
    public Result doWork() {
        AtomicInteger counter = new AtomicInteger(0);
        final List<DeviceMonitorItemContent.DeviceMonitorItem> oldDeviceList = new ArrayList<>(DeviceManager.getInstance().getDeviceList());
        final List<DeviceMonitorItemContent.DeviceMonitorItem> newDeviceList = new ArrayList<>();
        CountDownLatch cdl = new CountDownLatch(oldDeviceList.size());

        final QueryTaskExecutor<SystemQuery> exec = new QueryTaskExecutor<>();

        for (DeviceMonitorItemContent.DeviceMonitorItem deviceItem : oldDeviceList) {
            exec.executeAsync(new SystemQuery.SystemQueryRequest(deviceItem.getDeviceConfiguration()), systemQuery -> {
                if (systemQuery == null) {
                    Log.w(TAG, "no system query retrievable!");
                    // use old as fallback
                    systemQuery = deviceItem.systemQuery;
                    if (!CockpitStateManager.getInstance().isInTimeouts()
                            && !CockpitStateManager.getInstance().isConnecting()) {
                        if (deviceItem.deviceConfiguration.getSnmpVersion() < 3) {
                            SnmpManager.getInstance().resetV1Connection(deviceItem.deviceConfiguration);
                        } else {
                            SnmpManager.getInstance().resetV3Connection(deviceItem.deviceConfiguration);
                        }
                    }
                }
                newDeviceList.add(new DeviceMonitorItemContent.DeviceMonitorItem("#" + counter, deviceItem.host, deviceItem.port,
                        deviceItem.deviceConfiguration, systemQuery));
                counter.getAndIncrement();
                cdl.countDown();
            });
        }

        try {
            cdl.await(9000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // exchange listS
        DeviceManager.getInstance().getDeviceList().clear();
        DeviceManager.getInstance().getDeviceList().addAll(newDeviceList);

        return Result.success();
    }
}
