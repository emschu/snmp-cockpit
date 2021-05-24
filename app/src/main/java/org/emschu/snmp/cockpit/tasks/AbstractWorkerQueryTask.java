package org.emschu.snmp.cockpit.tasks;

import android.util.Log;

import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.ListQuery;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.TableQuery;
import org.emschu.snmp.cockpit.query.impl.CustomListQuery;
import org.emschu.snmp.cockpit.query.impl.DefaultListQuery;
import org.emschu.snmp.cockpit.query.view.AbstractCockpitQuerySection;
import org.emschu.snmp.cockpit.query.view.GroupedListQuerySection;
import org.emschu.snmp.cockpit.query.view.ListQuerySection;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractWorkerQueryTask {
    private final List<AbstractQueryRequest<? extends SnmpQuery>> snmpQueryList = new ArrayList<>();
    private final ManagedDevice managedDevice;

    public AbstractWorkerQueryTask(ManagedDevice managedDevice) {
        this.managedDevice = managedDevice;
    }

    public List<AbstractQueryRequest<? extends SnmpQuery>> getQueryList() {
        return snmpQueryList;
    }

    public abstract String getTabId();

    public final void execute() {
        // check constraints
        if (managedDevice == null) {
            throw new IllegalArgumentException("null DeviceConfiguration given");
        }
        if (this.snmpQueryList.isEmpty()) {
            Log.w(this.getClass().getSimpleName(), "Empty snmp query list. No processing will take place");
            return;
        }

        final CountDownLatch cdl = new CountDownLatch(this.snmpQueryList.size());
        Map<Integer, AbstractCockpitQuerySection> tabQueries = managedDevice.getSingleTabQueryCollection(this.getTabId());
        this.snmpQueryList.forEach(queryRequest -> {
            QueryTaskExecutor.executeAsync(queryRequest, result -> {
                if ((!(queryRequest instanceof CustomListQuery.CustomQueryRequest) && queryRequest.getContentTitleResourceId() == 0)
                        || (queryRequest instanceof CustomListQuery.CustomQueryRequest && ((CustomListQuery.CustomQueryRequest) queryRequest).getCustomQuery().getTitle().isEmpty())) {
                    Log.e(this.getClass().getSimpleName(), "String resource id is zero!");
                    return;
                }
                if (result instanceof TableQuery) {
                    TableQuery tq = (TableQuery) result;
                    tabQueries.put(tabQueries.size(), new GroupedListQuerySection(SnmpCockpitApp.getContext().getString(queryRequest.getContentTitleResourceId()), tq));
                } else if (result instanceof CustomListQuery && queryRequest instanceof CustomListQuery.CustomQueryRequest) {
                    CustomListQuery clq = (CustomListQuery) result;
                    CustomListQuery.CustomQueryRequest customQueryRequest = (CustomListQuery.CustomQueryRequest) queryRequest;
                    tabQueries.put(tabQueries.size(), new ListQuerySection(customQueryRequest.getCustomQuery().getTitle(), clq, true));
                } else if (result instanceof DefaultListQuery) {
                    ListQuery lq = (DefaultListQuery) result;
                    ListQuerySection querySection = new ListQuerySection(SnmpCockpitApp.getContext().getString(queryRequest.getContentTitleResourceId()), lq, true);
                    tabQueries.put(tabQueries.size(), querySection);
                } else {
                    Log.e(this.getClass().getSimpleName(), "Current query result is unknown!");
                }
                cdl.countDown();
            });
        });

        try {
            cdl.await(15L, TimeUnit.SECONDS);
            SnmpManager.getInstance().resetTimeout(managedDevice.getDeviceConfiguration());
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
            SnmpManager.getInstance().registerTimeout(managedDevice.getDeviceConfiguration());
        }
    }
}
