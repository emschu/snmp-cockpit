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

package org.emschu.snmp.cockpit.util;


import android.os.Handler;
import android.os.Looper;

/**
 * implementation of regular task
 */
public class PeriodicTask {
    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable internalWraperRunnable;

    private int INTERVAL = 10000;

    /**
     * constructor
     *
     * @param regularTask
     */
    private PeriodicTask(final Runnable regularTask) {
        internalWraperRunnable = new Runnable() {
            @Override
            public void run() {
                regularTask.run();
                handler.postDelayed(this, INTERVAL);
            }
        };
    }

    /**
     * constructor
     *
     * @param uiUpdater
     * @param interval
     */
    public PeriodicTask(Runnable uiUpdater, int interval){
        this(uiUpdater);
        INTERVAL = interval;
    }

    public synchronized void start(){
        internalWraperRunnable.run();
    }

    public synchronized void stop(){
        handler.removeCallbacks(internalWraperRunnable);
    }
}