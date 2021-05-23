package org.emschu.snmp.cockpit.tasks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractWorker extends Worker {
    private final ManagedDevice managedDevice;
    public final String TAG = this.getClass().getSimpleName();

    public AbstractWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);

        String deviceId = workerParams.getInputData().getString("device_id");
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(DetailInfoQueryTask.class.getSimpleName(), "Empty deviceId received!");
            this.managedDevice = null;
        } else {
            this.managedDevice = DeviceManager.getInstance().getDevice(deviceId);
        }
    }

    public ManagedDevice getManagedDevice() {
        return managedDevice;
    }

    public abstract AbstractWorkerQueryTask getTask();

    @NonNull
    @Override
    public Result doWork() {
        if (this.managedDevice == null) {
            Log.e(TAG, "Managed device is null");
            return Result.failure();
        }
        // register queries
        try {
            // register queries
            AbstractWorkerQueryTask task = getTask();
            task.execute();
        } catch (Exception ex) {
            Log.e(TAG, "Worker-Problem detected: " + ex.getMessage());
            // TODO is this a timeout issue?
            SnmpManager.getInstance().registerTimeout(getManagedDevice().getDeviceConfiguration());
            return Result.failure();
        } finally {
            this.cleanUp();
        }
        return Result.success();
    }

    public void cleanUp() {
        // overwrite this method in subclasses - if needed
    }
}
