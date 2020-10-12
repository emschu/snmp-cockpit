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

package org.emschu.snmp.cockpit.activity;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Observer;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.emschu.snmp.cockpit.CockpitMainActivity;
import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.service.CockpitStateService;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.emschu.snmp.cockpit.util.BooleanObservable;

/**
 * this interface adds security mechanism of the app to an activity.
 * We use an interface to spread our security mechanisms through the app.
 * This class should be called a trait.
 *
 * Usage:
 *  - {@link #initObservables(Activity, AlertHelper, OnSecurityStateChangeListener)} in #onCreate of your activity.
 *  - and add this to your activity:
 *     \@Override
 *     protected void onStart() {
 *         super.onStart();
 *         startProtection(this);
 *     }
 *
 *     \@Override
 *     protected void onRestart() {
 *         super.onRestart();
 *         restartTrigger(this);
 *     }
 *
 */
public interface ProtectedActivity {

    /**
     * magic init method to setup alert window handling for an activity
     *
     * @param activity
     * @param alertHelper
     * @param listener
     */
    default void initObservables(@NonNull Activity activity, @NonNull AlertHelper alertHelper,
                                 @Nullable OnSecurityStateChangeListener listener) {
        Log.d(ProtectedActivity.class.getName(), "observables inited for " + this.getClass().getSimpleName());
        if (CockpitStateManager.getInstance().isInTestMode()) {
            Log.d(ProtectedActivity.class.getName(), "Test mode detected - security disabled");
            return;
        }

        BooleanObservable isNetworkSecureObservable = CockpitStateManager.getInstance().getNetworkSecurityObservable();
        BooleanObservable isInTimeoutObservable = CockpitStateManager.getInstance().getIsInTimeoutsObservable();
        BooleanObservable isInSessionTimeoutObservable = CockpitStateManager.getInstance().getIsInSessionTimeoutObservable();
        // ensure only one observer at the same time
        isNetworkSecureObservable.deleteObservers();
        isInTimeoutObservable.deleteObservers();
        isInSessionTimeoutObservable.deleteObservers();

        isNetworkSecureObservable.addObserver(getNetworkSecurityObserver(activity, alertHelper, listener));
        isInTimeoutObservable.addObserver(getConnectionTimeoutObserver(alertHelper));
        isInSessionTimeoutObservable.addObserver(getIsInSessionTimeoutObserver(alertHelper));

        checkSessionTimeout();
    }

    /**
     * method to check session timeout state
     */
    default void checkSessionTimeout() {
        CockpitPreferenceManager cockpitPreferenceManager = SnmpCockpitApp.getPreferenceManager();
        cockpitPreferenceManager.checkSessionTimeout();
    }

    /**
     * call this in #onStop of activities!
     *
     * @param activity
     */
    default void stopProtection(Activity activity) {
        Log.d(ProtectedActivity.class.getName(), "stop cockpit service in " + this.getClass().getName());
    }

    /**
     * call this in #onStart of activities!
     *
     * @param activity
     */
    default void startProtection(Activity activity) {
        if (CockpitStateManager.getInstance().isInTestMode()) {
            Log.d(ProtectedActivity.class.getName(), "Test mode detected - security disabled");
            return;
        }
        Log.d(ProtectedActivity.class.getName(), "start cockpit service in " + this.getClass().getSimpleName());
        CockpitStateService.enqueueWork(activity, new Intent());
    }

    /**
     * should be called in #onRestart
     *
     * @param activity
     */
    default void restartTrigger(Activity activity) {
        Log.d(ProtectedActivity.class.getName(), "restart trigger called");
        startProtection(activity);
        checkSessionTimeout();
    }

    /**
     * this method is called when use has clicked "retry connection" after connection timeout
     */
    public void restartQueryCall();

    /**
     * Generic method of network security observer
     *
     * @param activity
     * @param alertHelper
     * @param listener
     * @return
     */
    default Observer getNetworkSecurityObserver(Activity activity, AlertHelper alertHelper, OnSecurityStateChangeListener listener) {
        return (booleanObservable, arg) -> {
            boolean isNetworkSecureState = ((BooleanObservable) booleanObservable).getValue();
            Log.d(ProtectedActivity.class.getName(), "observable changed! new value: " + isNetworkSecureState);
            if (((BooleanObservable) booleanObservable).getValue()) {
                Toast.makeText(activity, R.string.secure_network_toast, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, R.string.not_secure_network_toast, Toast.LENGTH_SHORT).show();
            }

            if (!isNetworkSecureState) {
                // immediately stop running connection tasks
                if (listener != null) {
                    listener.onInsecureState();
                }

                ThreadPoolExecutor threadPoolExecutor = SnmpManager.getInstance().getThreadPoolExecutor();
                if (threadPoolExecutor.getActiveCount() > 0) {
                    List<Runnable> runnables = threadPoolExecutor.shutdownNow();
                    Log.d(ProtectedActivity.class.getName(), "stopped thread pool with jobs: " + runnables.toString());
                }
                alertHelper.showNotSecureAlert();
            } else {
                if (listener != null) {
                    listener.onSecureState();
                }
                alertHelper.closeAllSecurityAlerts();
            }
        };
    }

    /**
     * generic method of network timeout observer
     *
     * @param alertHelper
     * @return
     */
    default Observer getConnectionTimeoutObserver(AlertHelper alertHelper) {
        return (observable, arg) -> {
            boolean isInTimeouts = ((BooleanObservable) observable).getValue();
            Log.d(ProtectedActivity.class.getName(), "timeout observable changed: " + isInTimeouts);
            if (isInTimeouts) {
                alertHelper.showDeviceTimeoutDialog();
            } else {
                alertHelper.closeAllTimeoutAlerts();
            }
        };
    }

    /**
     * generic method of app session timeout observer
     *
     * @param alertHelper
     * @return
     */
    default Observer getIsInSessionTimeoutObserver(AlertHelper alertHelper) {
        return (observable, arg) -> {
            boolean isInSessionTimeout = ((BooleanObservable) observable).getValue();
            Log.d(ProtectedActivity.class.getName(), "session timeout observable changed: " + isInSessionTimeout);
            if (isInSessionTimeout) {
                ThreadPoolExecutor threadPoolExecutor = SnmpManager.getInstance().getThreadPoolExecutor();
                if (threadPoolExecutor.getActiveCount() > 0) {
                    List<Runnable> runnables = threadPoolExecutor.shutdownNow();
                    Log.d(ProtectedActivity.class.getName(), "stopped thread pool with jobs: " + runnables.toString());
                }
                DeviceManager.getInstance().removeAllItems();
                alertHelper.showSessionTimeoutDialog();
            } else {
                alertHelper.closeAllTimeoutAlerts();
            }
        };
    }

    /**
     * helper interface to hook into network security alert actions
     */
    interface OnSecurityStateChangeListener {
        public void onInsecureState();

        public void onSecureState();
    }
}
