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

package org.emschu.snmp.cockpit.service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.network.WifiNetworkManager;
import org.emschu.snmp.cockpit.util.BooleanObservable;

/**
 * observes network security
 */
public class CockpitStateService extends android.app.Service {
    public static final String TAG = CockpitStateService.class.getName();
    private ConnectivityManager.NetworkCallback callback;
    private ConnectivityManager cm;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // we don't bind sth
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "starting cockpit state service");

        cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            throw new IllegalStateException("no connectivity manager available");
        }

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        final BooleanObservable isNetworkSecureObservable = CockpitStateManager.getInstance().getNetworkSecurityObservable();
        final WifiNetworkManager wifiNetworkManager = WifiNetworkManager.getInstance(this);

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.w(TAG, "onAvailable()");
                checkState();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.w(TAG, "onLost()");
                checkState();
            }

            private void checkState() {
                Log.d("app", "Network connectivity change");

                if (CockpitStateManager.getInstance().isInTestMode()) {
                    Log.d(TAG, "Test mode for network detected - security disabled");
                    return;
                }

                wifiNetworkManager.updateMode();

                isNetworkSecureObservable.setValue(wifiNetworkManager.isNetworkSecure());
                isNetworkSecureObservable.notifyObservers();
                Log.d(TAG, "network receive event finished");
            }
        };

        cm.registerNetworkCallback(builder.build(), callback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "finishing cockpit state service");
        cm.unregisterNetworkCallback(callback);
    }
}
