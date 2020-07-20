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

package org.emschu.snmp.cockpit;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * simple application class which provides the context within the app
 */
public class SnmpCockpitApp extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static CockpitPreferenceManager cockpitPreferenceManager;

    public static CockpitStateManager getCockpitStateManager() {
        return cockpitStateManager;
    }

    private static void setCockpitStateManager(CockpitStateManager cockpitStateManager) {
        SnmpCockpitApp.cockpitStateManager = cockpitStateManager;
    }

    private static CockpitStateManager cockpitStateManager;

    public static CockpitPreferenceManager getPreferenceManager() {
        return cockpitPreferenceManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setContext(this);
        setCockpitPreferenceManager(new CockpitPreferenceManager(context));
        setCockpitStateManager(CockpitStateManager.getInstance());
    }

    private static void setCockpitPreferenceManager(CockpitPreferenceManager cockpitPreferenceManager) {
        SnmpCockpitApp.cockpitPreferenceManager = cockpitPreferenceManager;
    }

    private static void setContext(Context context) {
        SnmpCockpitApp.context = context;
    }

    public static Context getContext() {
        return context;
    }
}
