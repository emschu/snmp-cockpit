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

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.fragment.app.Fragment;
import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.snmp.NoDeviceException;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

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
    protected void initQueryView(CockpitQueryView cockpitQueryView) {
        this.cockpitQueryView = cockpitQueryView;
    }

    /**
     * this method retrieves the {@link ManagedDevice} object of this class out of fragment args
     *
     * @return
     */
    public ManagedDevice getManagedDevice() {
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

    /**
     * async waiter with timeout handling
     *
     * @param queryTask
     * @param deviceConfiguration
     */
    protected void waitForTaskResultAsync(AsyncTask<?,?,?> queryTask, DeviceConfiguration deviceConfiguration) {
        (new Handler(Looper.getMainLooper())).post(() -> {
            try {
                int offset = deviceConfiguration.getAdditionalTimeoutOffset();
                queryTask.get((long) CockpitPreferenceManager.TIMEOUT_WAIT_ASYNC_MILLISECONDS + offset, TimeUnit.MILLISECONDS);
                SnmpManager.getInstance().resetTimeout(deviceConfiguration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                Log.e(TAG, "execution exception: " + e.getMessage());
            } catch (TimeoutException e) {
                Log.w(TAG, "timeout reached!");
                SnmpManager.getInstance().registerTimeout(deviceConfiguration);
            }
        });
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