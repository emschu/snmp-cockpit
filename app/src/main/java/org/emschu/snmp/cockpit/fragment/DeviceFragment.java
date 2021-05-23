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

package org.emschu.snmp.cockpit.fragment;

import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.query.view.AbstractCockpitQuerySection;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.snmp.NoDeviceException;

import java.util.Map;

/**
 * all tab fragments should extend this
 */
public abstract class DeviceFragment extends Fragment {
    public static final String TAG = DeviceFragment.class.getName();
    private ManagedDevice managedDevice = null;

    private CockpitQueryView cockpitQueryView;

    public DeviceFragment() {
    }

    /**
     * implement this method in subclasses
     * this event is triggered when tabs are reloaded. so reload query view there
     */
    public abstract void reloadData();

    public void setManagedDevice(ManagedDevice managedDevice) {
        this.managedDevice = managedDevice;
    }

    @Override
    public void onStart() {
        super.onStart();
        reloadData();
    }

    /**
     * init query view in this class
     *
     * @param cockpitQueryView
     */
    protected void initQueryView(final CockpitQueryView cockpitQueryView) {
        this.cockpitQueryView = cockpitQueryView;
    }

    /**
     * this method retrieves the {@link ManagedDevice} object of this class out of fragment args
     *
     * @return
     * @throws NoDeviceException
     */
    public ManagedDevice getManagedDevice() throws NoDeviceException {
        if (managedDevice == null) {
            String deviceId;
            // this should work in embedded activity mode and in pure fragment mode
            if (getArguments() == null && getActivity() != null) {
                deviceId = getActivity().getIntent().getStringExtra(TabbedDeviceActivity.EXTRA_DEVICE_ID);
            } else {
                deviceId = getArguments().getString(TabbedDeviceActivity.EXTRA_DEVICE_ID, null);
            }
            if (deviceId == null || deviceId.isEmpty()) {
                throw new IllegalStateException("no fragment argument EXTRA_DEVICE_ID set");
            }
            Log.d(TAG, "looking for id:" + deviceId);
            ManagedDevice managedDeviceObject = DeviceManager.getInstance().getDevice(deviceId);
            if (managedDeviceObject != null) {
                this.managedDevice = managedDeviceObject;
            } else {
                throw new NoDeviceException("no managed device with id " + TabbedDeviceActivity.EXTRA_DEVICE_ID);
            }
        }
        return managedDevice;
    }


    protected void startQueryTasks(CockpitQueryView queryView, ManagedDevice md, Class<? extends ListenableWorker> workerClass, String tabId) {
        Data queryTaskTabInput = new Data.Builder().putString("device_id", md.getDeviceConfiguration().getUniqueDeviceId()).build();
        // TODO add some more useful constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(constraints)
                .setInputData(queryTaskTabInput).build();

        // clear current collection
        Map<Integer, AbstractCockpitQuerySection> singleTabQueryCollection = md.getSingleTabQueryCollection(tabId);
        if (singleTabQueryCollection != null && !singleTabQueryCollection.isEmpty()) {
            singleTabQueryCollection.clear();
        }

        FragmentActivity activity = getActivity();
        if (activity != null) {
            WorkManager instance = WorkManager.getInstance(activity);
            instance.enqueue(workRequest);
            instance.getWorkInfoByIdLiveData(workRequest.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            queryView.getCockpitQuerySectionMap().clear();
                            if (!md.getSingleTabQueryCollection(tabId).isEmpty()) {
                                queryView.getCockpitQuerySectionMap().putAll(md.getSingleTabQueryCollection(tabId));
                            }
                            queryView.render(true);
                        }
                    });
        }
    }

    /**
     * @param listener
     */
    public void setOnRenderingFinishedListener(CockpitQueryView.OnRenderingFinishedListener listener) {
        CockpitQueryView queryView = getQueryView();
        if (queryView == null) {
            Log.w(TAG, "can not set rendering finished listener on null query view");
            return;
        }
        queryView.setOnRenderingFinishedListener(listener);
    }

    public CockpitQueryView getQueryView() {
        return cockpitQueryView;
    }

    public String getDeviceId() {
        return getManagedDevice().getDeviceConfiguration().getUniqueDeviceId();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cockpitQueryView = null;
        managedDevice = null;
    }
}