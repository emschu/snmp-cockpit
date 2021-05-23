package org.emschu.snmp.cockpit.tasks;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.SnmpConnection;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.snmp4j.smi.OID;

import java.util.List;

/**
 * This worker implementation handles the connection test task which is started right after the user starts a
 * connection attempt.
 */
public class DeviceAddTask extends Worker {
    private static final String TAG = DeviceAddTask.class.getSimpleName();
    private final DeviceConfiguration usedDeviceConfiguration;
    private final int connectionTestTimeout;
    private final int connectionTestRetries;
    private final int connectionTestTotal;

    // progress data keys
    public static final String PROGRESS_CURRENT_NUM = "PROGRESS_CURRENT";
    public static final String PROGRESS_ALL_NUM = "PROGRESS_ALL";
    // return output data keys
    public static final String OUTPUT_DOES_EXIST = "DOES_EXIST"; // optional

    /**
     * @param context
     * @param workerParams
     */
    public DeviceAddTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        this.connectionTestTotal = SnmpManager.getInstance().getTotalConnectionTestCount();
        this.connectionTestRetries = SnmpCockpitApp.getPreferenceManager().getConnectionTestRetries();
        this.connectionTestTimeout = SnmpCockpitApp.getPreferenceManager().getConnectionTestTimeout();

        this.usedDeviceConfiguration = CockpitStateManager.getInstance().getTestDeviceConfiguration();
        if (this.usedDeviceConfiguration == null) {
            throw new IllegalStateException("No device configuration available!");
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        SnmpManager snmpManager = SnmpManager.getInstance();

        // avoid side effects:
        if (snmpManager.doesConnectionExist(usedDeviceConfiguration.getUniqueDeviceId())) {
            Log.d(TAG, "Connection does already exist");
            return Result.failure(new Data.Builder().putBoolean(OUTPUT_DOES_EXIST, true).build());
        }

        // for v3 connections do a (possibly large) connection test to get correct auth and privProtocol
        if (usedDeviceConfiguration.getSnmpVersionEnum() == DeviceConfiguration.SNMP_VERSION.v3) {
            Log.d(TAG, "start connection check v3");
            setProgressAsync(new Data.Builder().putInt(PROGRESS_CURRENT_NUM, 0).putInt(PROGRESS_ALL_NUM, this.connectionTestTotal).build());

            Pair<OID, OID> firstCombination;
            if (usedDeviceConfiguration.isConnectionTestNeeded()) {
                Log.d(TAG, "run connection test");
                List<Pair<OID, OID>> workingSecuritySettings =
                        SnmpManager.getInstance().testConnections(usedDeviceConfiguration, (counter) -> {
                            setProgressAsync(new Data.Builder().putInt(PROGRESS_CURRENT_NUM, counter)
                                    .putInt(PROGRESS_ALL_NUM, this.connectionTestTotal).build());
                        }, connectionTestTimeout, connectionTestRetries);
                if (workingSecuritySettings.isEmpty()) {
                    Log.d(TAG, "no auth and privProtocol matched");
                    return Result.failure();
                }
                firstCombination = workingSecuritySettings.get(0);
            } else {
                Log.d(TAG, "skip connection test");
                firstCombination = new Pair<>(usedDeviceConfiguration.getAuthProtocol(), usedDeviceConfiguration.getPrivProtocol());
            }
            // TODO algorithm to use strongest
            Log.d(TAG, "selected authProtocol: " + firstCombination.first + " and privProtocol: " + firstCombination.second);
            usedDeviceConfiguration.setAuthProtocol(firstCombination.first);
            usedDeviceConfiguration.setPrivProtocol(firstCombination.second);
        }
        SnmpConnection connector = SnmpManager.getInstance().getOrCreateConnection(usedDeviceConfiguration);
        // try it 2 times
        if (connector == null) {
            connector = SnmpManager.getInstance().getOrCreateConnection(usedDeviceConfiguration);
            if (connector == null) {
                Log.w(TAG, "no connection available in device test task");
                return Result.failure();
            }
        }
        if (!connector.canPing(usedDeviceConfiguration)) {
            connector.close();
            return Result.failure();
        }
        return Result.success();
    }
}
