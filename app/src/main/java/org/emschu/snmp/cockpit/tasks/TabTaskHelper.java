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

import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.TableQuery;
import org.emschu.snmp.cockpit.query.impl.DefaultListQuery;
import org.emschu.snmp.cockpit.query.view.CockpitQueryView;
import org.emschu.snmp.cockpit.query.view.GroupedListQuerySection;
import org.emschu.snmp.cockpit.query.view.ListQuerySection;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

/**
 * this interface provides methods for tab tasks so we can use them whithout extending android
 * {@link android.os.AsyncTask}
 * <p>
 * this is more a "trait" as an interface and the price we pay is: default interface methods need to be public
 * TODO move to an abstract task class?
 */
public interface TabTaskHelper {

    String TAB_TASK_TAG = "TabTask";

    public void cancelTasks();

    /**
     * simple helper method
     *
     * @param qt
     * @return
     */
    public default SnmpQuery getAnswer(QueryTask<?> qt) {
        try {
            if (qt.getDeviceConfiguration() == null) {
                throw new IllegalStateException("null device config given");
            }
            int offset = qt.getDeviceConfiguration().getAdditionalTimeoutOffset();
            SnmpQuery query = qt.get((long) CockpitPreferenceManager.TIMEOUT_WAIT_ASYNC_MILLISECONDS + offset, TimeUnit.MILLISECONDS);
            if (query != null && qt.getDeviceConfiguration() != null) {
                SnmpManager.getInstance().resetTimeout(qt.getDeviceConfiguration());
                return query;
            }
        } catch (RuntimeException | InterruptedException e) {
            Log.e(TAB_TASK_TAG, "detail task interrupted!");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Log.e(TAB_TASK_TAG, "detail task failed: " + e.getMessage());
        } catch (TimeoutException e) {
            Log.w(TAB_TASK_TAG, "timeout reached for task: detail info query task");
            if (qt.getDeviceConfiguration() != null) {
                Log.d(TAB_TASK_TAG, "cancel all running tasks");
                cancelTasks();
                SnmpManager.getInstance().registerTimeout(qt.getDeviceConfiguration());
            }
        }
        return null;
    }

    /**
     * generic method to add a table section to this query view
     *
     * @param title
     * @param queryTask
     * @param queryView
     */
    public default void addTableSection(String title,
                                        QueryTask<? extends SnmpQuery> queryTask,
                                        AtomicReference<CockpitQueryView> queryView) {
        TableQuery snmpQuery = (TableQuery) getAnswer(queryTask);
        if (snmpQuery != null) {
            queryView.get().addQuerySection(new GroupedListQuerySection(title, snmpQuery));
        }
    }

    /**
     * generic method to add a list query
     *
     * @param title
     * @param infoTask
     * @param queryView
     */
    public default void addListQuery(String title,
                                     QueryTask<? extends SnmpQuery> infoTask,
                                     AtomicReference<CockpitQueryView> queryView) {
        DefaultListQuery ipInfoListQuery = (DefaultListQuery) getAnswer(infoTask);
        if (ipInfoListQuery != null) {
            ListQuerySection querySection = new ListQuerySection(title, ipInfoListQuery);
            querySection.setCollapsible(true);
            queryView.get().addQuerySection(querySection);
        }
    }
}
