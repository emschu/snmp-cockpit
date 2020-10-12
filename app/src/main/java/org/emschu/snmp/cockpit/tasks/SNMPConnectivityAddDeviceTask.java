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
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.snmp4j.smi.OID;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.emschu.snmp.cockpit.CockpitMainActivity;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.fragment.DeviceMonitorViewFragment;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.SnmpConnection;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * check snmp connectivity and add device to app
 * <p>
 * if query response is null no connection possible
 */
public class SNMPConnectivityAddDeviceTask extends AsyncTask<Void, Void, Boolean> {

    public static final String TAG = SNMPConnectivityAddDeviceTask.class.getName();

    private final DeviceMonitorViewFragment deviceMonitorViewFragment;
    private final int connectionTestTimeout;
    private final int connectionTestRetries;
    private final DeviceConfiguration usedDeviceConfiguration;
    private WeakReference<LinearLayout> progressRow;
    private boolean doesConnectionExist = false;
    private int connectionTestTotal = 0;

    /**
     * constructor
     *
     * @param deviceMonitorViewFragment
     * @param progressRow
     */
    public SNMPConnectivityAddDeviceTask(DeviceConfiguration deviceConfiguration,
                                         DeviceMonitorViewFragment deviceMonitorViewFragment, LinearLayout progressRow) {
        this.usedDeviceConfiguration = deviceConfiguration;
        this.deviceMonitorViewFragment = deviceMonitorViewFragment;
        this.progressRow = new WeakReference<>(progressRow);
        connectionTestTotal = SnmpManager.getInstance().getTotalConnectionTestCount();

        this.connectionTestRetries = SnmpCockpitApp.getPreferenceManager().getConnectionTestRetries();
        this.connectionTestTimeout = SnmpCockpitApp.getPreferenceManager().getConnectionTestTimeout();
    }

    /**
     * background device test tasks to prepare communication
     *
     * @param voids
     * @return
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        SnmpManager snmpManager = SnmpManager.getInstance();

        // avoid side effects:
        doesConnectionExist = false;
        if (snmpManager.doesConnectionExist(usedDeviceConfiguration.getUniqueDeviceId())) {
            Log.d(TAG, "connection does already exist");
            doesConnectionExist = true;
            return false;
        }
        Log.d(TAG, "connection is new");

        // for v3 connections do a (possibly large) connection test to get correct auth and privProtocol
        if (usedDeviceConfiguration.getSnmpVersionEnum() == DeviceConfiguration.SNMP_VERSION.v3) {
            Log.d(TAG, "start connection check v3");
            publishProgress();
            Pair<OID, OID> firstCombination;
            if (usedDeviceConfiguration.isConnectionTestNeeded()) {
                Log.d(TAG, "run connection test");
                List<Pair<OID, OID>> workingSecuritySettings =
                        SnmpManager.getInstance().testConnections(usedDeviceConfiguration,
                                this::publishProgress, connectionTestTimeout, connectionTestRetries);
                if (workingSecuritySettings.isEmpty()) {
                    Log.d(TAG, "no auth and privProtocol matched");
                    return false;
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
                return false;
            }
        }
        if (!connector.canPing(usedDeviceConfiguration)) {
            connector.close();
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        Log.d(TAG, "progress update");
        TextView infoTextView = this.progressRow.get().findViewById(R.id.connection_attempt_count_label);
        infoTextView.setText(String.format(getCurrentLocale(SnmpCockpitApp.getContext()), "%s %d/%d",
                SnmpCockpitApp.getContext().getString(R.string.connection_attempt_label),
                SnmpManager.getInstance().getCurrentConnectionTestsDoneCount(),
                connectionTestTotal)
        );
    }

    /**
     * method for current locale
     *
     * @param context
     * @return
     */
    @SuppressWarnings({"deprecation"})
    protected Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        } else {
            return context.getResources().getConfiguration().getLocales().get(0);
        }
    }

    @Override
    protected void onPostExecute(Boolean s) {
        super.onPostExecute(s);
        if (s == null || !s) {
            Log.e(TAG, "Connection test NOT successful! Could not connect!");
            if (doesConnectionExist) {
                Toast.makeText(SnmpCockpitApp.getContext(),
                        R.string.connection_already_exists, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SnmpCockpitApp.getContext(),
                        R.string.connection_test_not_successful_label, Toast.LENGTH_LONG).show();
            }
//            AsyncTask.execute(() -> SnmpManager.getInstance().removeConnection(usedDeviceConfiguration));
        } else {
            Log.i(TAG, "Connection test successful");
            Toast.makeText(SnmpCockpitApp.getContext(),
                    SnmpCockpitApp.getContext().getString(R.string.connection_test_successful_label), Toast.LENGTH_SHORT).show();

            DeviceManager.getInstance().add(usedDeviceConfiguration, false);
            // refresh ui on list change!
            RecyclerView.Adapter<?> adapter = deviceMonitorViewFragment.getRecyclerView().getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            if (SnmpCockpitApp.getContext() instanceof CockpitMainActivity) {
                CockpitMainActivity cockpitMainActivity = (CockpitMainActivity) SnmpCockpitApp.getContext();
                cockpitMainActivity.checkNoData();
            } else {
                Log.w(TAG, "CockpitMainActivity context expected!");
            }
        }
        progressRow.get().setVisibility(View.GONE);
        TextView infoTextView = this.progressRow.get().findViewById(R.id.connection_attempt_count_label);
        infoTextView.setText(SnmpCockpitApp.getContext().getString(R.string.connection_attempt_label));
        CockpitStateManager.getInstance().setConnecting(false);
        CockpitStateManager.getInstance().setConnectionTask(null);
    }

    public void setProgressRow(LinearLayout progressRow) {
        this.progressRow = new WeakReference<>(progressRow);
        publishProgress();
    }
}