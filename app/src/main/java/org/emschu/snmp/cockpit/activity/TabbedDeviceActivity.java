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

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.adapter.DeviceSpinnerAdapter;
import org.emschu.snmp.cockpit.adapter.ViewPager2Adapter;
import org.emschu.snmp.cockpit.fragment.DeviceFragment;
import org.emschu.snmp.cockpit.fragment.tabs.DeviceDetailFragment;
import org.emschu.snmp.cockpit.fragment.tabs.HardwareQueryFragment;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.query.impl.general.SystemQuery;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.util.PeriodicTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Activity for single device view
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TabbedDeviceActivity extends ProtectedActivity {

    public static final String EXTRA_DEVICE_ID = "device_id";
    public static final String EXTRA_IS_COLLAPSED = "is_collapsed";
    public static final String EXTRA_OPEN_TAB_OID = "open_tab_oid";
    public static final String TAG = TabbedDeviceActivity.class.getName();
    private ViewPager2 viewPager;
    private ViewPager2Adapter viewPagerAdapter;
    private ManagedDevice managedDevice;
    private boolean isCollapsed;
    private ProgressBar deviceProgressBar;
    private AlertHelper alertHelper;
    private PeriodicTask periodicTask = new PeriodicTask(this::checkSecurity, 2500);
    private CockpitDbHelper dbHelper;
    private TabLayoutMediator tabLayoutMediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed_device);

        String deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        alertHelper = new AlertHelper(this);
        initObservables(alertHelper, null);
        dbHelper = new CockpitDbHelper(this);

        List<ManagedDevice> managedDevices = DeviceManager.getInstance().getManagedDevices();
        // build device array
        String[] deviceList = new String[managedDevices.size()];
        int targetPosition = 0;
        for (int i = 0; i < managedDevices.size(); i++) {
            if (managedDevices.get(i).getDeviceConfiguration().getUniqueDeviceId().equals(deviceId)) {
                targetPosition = i;
            }
            deviceList[i] = managedDevices.get(i).getShortDeviceLabel();
        }
        if (!managedDevices.isEmpty()) {
            managedDevice = managedDevices.get(targetPosition);
        }
        if (managedDevice == null || deviceId == null || deviceId.trim().isEmpty()) {
            Log.e(TAG, "null device!");
            return;
        }

        Log.d(TAG, "init: " + managedDevice.getDeviceConfiguration().getUniqueDeviceId());
        // init view pager
        viewPager = findViewById(R.id.device_info_container_viewpager);
        initViewPagerAdapter(deviceId);

        CockpitPreferenceManager cockpitPreferenceManager = SnmpCockpitApp.getPreferenceManager();
        if (cockpitPreferenceManager.isPeriodicUpdateEnabled()) {
            periodicTask = new PeriodicTask(
                    () -> refreshView(true), cockpitPreferenceManager.getUiUpdateSeconds() * 1000);
        } else {
            periodicTask = new PeriodicTask(this::checkSecurity, 2500);
        }

        // Setup spinner
        Spinner spinner = findViewById(R.id.device_spinner);
        spinner.setAdapter(new DeviceSpinnerAdapter(
                toolbar.getContext(),
                deviceList)
        );

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // When the given dropdown item is selected, show its contents in the
                // container view.
                if (managedDevices.get(position) != null) {
                    managedDevice = managedDevices.get(position);
                }
                Log.d(TAG, "choose " + managedDevice.getDeviceConfiguration().getUniqueDeviceId());
                // updating system query information of managed device
                DeviceManager.getInstance().updateSystemQueryAsync(managedDevice);
                updateDeviceInformation();
                initViewPagerAdapter(managedDevice.getDeviceConfiguration().getUniqueDeviceId());
                reloadTabData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        initHeadTable();

        updateDeviceInformation();

        if (targetPosition != spinner.getSelectedItemPosition()) {
            spinner.setSelection(targetPosition);
        }

        deviceProgressBar = findViewById(R.id.device_progress_bar);
        deviceProgressBar.setVisibility(View.GONE);
    }

    /**
     * alertHelper method which handles head table folding + icon changes
     * <p>
     * method {@link #updateDeviceInformation()} should run AFTER this method vor visibility issues
     */
    private void initHeadTable() {
        TableRow tableRow1 = findViewById(R.id.device_detail_row_1);
        TableRow tableRow2 = findViewById(R.id.device_detail_row_2);
        TableRow tableRow3 = findViewById(R.id.device_detail_row_3);
        TableRow tableRow4 = findViewById(R.id.device_detail_row_4);
        TableRow tableRow5 = findViewById(R.id.device_detail_row_5);
        TableRow tableRow6 = findViewById(R.id.device_detail_row_6);
        TableRow tableRow7 = findViewById(R.id.device_detail_row_7);

        TextView deviceDetailLabel = findViewById(R.id.device_detail_device_label);

        tableRow1.setOnClickListener(v -> {
            boolean isShowing = tableRow2.getVisibility() == View.VISIBLE;
            if (isShowing) {
                isCollapsed = true;
                deviceDetailLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(this, R.drawable.ic_keyboard_arrow_right_black),
                        null, null, null);
                tableRow2.setVisibility(View.GONE);
                tableRow3.setVisibility(View.GONE);
                tableRow4.setVisibility(View.GONE);
                tableRow5.setVisibility(View.GONE);
                tableRow6.setVisibility(View.GONE);
                tableRow7.setVisibility(View.GONE);
            } else {
                isCollapsed = false;
                deviceDetailLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(this, R.drawable.ic_keyboard_arrow_down_black),
                        null, null, null);
                tableRow2.setVisibility(View.VISIBLE);
                tableRow3.setVisibility(View.VISIBLE);
                tableRow4.setVisibility(View.VISIBLE);
                tableRow5.setVisibility(View.VISIBLE);
                tableRow6.setVisibility(View.VISIBLE);
                tableRow7.setVisibility(View.VISIBLE);

                // we need to call this method here immediately after its visible
                updateDeviceInformation();
            }
        });

        // initial collapse
        tableRow1.callOnClick();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startProtection(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restartTrigger(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (periodicTask != null) {
            periodicTask.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (periodicTask != null) {
            periodicTask.stop();
        }
    }

    /**
     * method to load main deviec detail data which is shown over tab bar
     */
    private void updateDeviceInformation() {
        Log.d(TAG, "update general device information");
        TextView deviceLabel = findViewById(R.id.device_detail_device_label);
        if (managedDevice == null) {
            return;
        }
        deviceLabel.setText(managedDevice.getDeviceLabel());

        TextView userOrCommunityLabel = findViewById(R.id.device_detail_user_label);
        if (managedDevice.getDeviceConfiguration().getSnmpVersionEnum() == DeviceConfiguration.SNMP_VERSION.v3) {
            userOrCommunityLabel.setText(R.string.user);
        } else {
            userOrCommunityLabel.setText(R.string.community_label);
        }

        TextView userLabel = findViewById(R.id.device_detail_sys_ip);
        userLabel.setText(managedDevice.getDeviceConfiguration().getUsername());

        TextView sysDescrLabel = findViewById(R.id.device_detail_sys_descr);
        SystemQuery initialSystemQuery = managedDevice.getLastSystemQuery();
        if (initialSystemQuery == null) {
            throw new IllegalStateException("no system query given!");
        }
        sysDescrLabel.setText(initialSystemQuery.getSysDescr());
        if (initialSystemQuery.getSysDescr() == null || initialSystemQuery.getSysDescr().isEmpty()) {
            findViewById(R.id.device_detail_row_3).setVisibility(View.GONE);
        } else {
            if (!isCollapsed) {
                findViewById(R.id.device_detail_row_3).setVisibility(View.VISIBLE);
            }
        }

        TextView sysLocationLabel = findViewById(R.id.device_detail_sys_location);
        String sysLocation = initialSystemQuery.getSysLocation();
        sysLocationLabel.setText(sysLocation);
        if (sysLocation == null || sysLocation.isEmpty()) {
            findViewById(R.id.device_detail_row_4).setVisibility(View.GONE);
        } else {
            if (!isCollapsed) {
                findViewById(R.id.device_detail_row_4).setVisibility(View.VISIBLE);
            }
        }

        TextView sysContactLabel = findViewById(R.id.device_detail_sys_contact);
        sysContactLabel.setText(initialSystemQuery.getSysContact());
        if (initialSystemQuery.getSysContact() == null || initialSystemQuery.getSysContact().isEmpty()) {
            findViewById(R.id.device_detail_row_6).setVisibility(View.GONE);
        } else {
            if (!isCollapsed) {
                findViewById(R.id.device_detail_row_6).setVisibility(View.VISIBLE);
            }
        }

        TextView sysUpTimeLabel = findViewById(R.id.device_detail_sys_uptime);
        sysUpTimeLabel.setText(initialSystemQuery.getSysUpTime());
        if (initialSystemQuery.getSysUpTime() == null || initialSystemQuery.getSysUpTime().isEmpty()) {
            findViewById(R.id.device_detail_row_5).setVisibility(View.GONE);
        } else {
            if (!isCollapsed) {
                findViewById(R.id.device_detail_row_5).setVisibility(View.VISIBLE);
            }
        }

        TextView sysObjectIdLabel = findViewById(R.id.device_detail_sys_object_id);
        sysObjectIdLabel.setText(initialSystemQuery.getSysObjectId());
        if (initialSystemQuery.getSysObjectId() == null || initialSystemQuery.getSysObjectId().isEmpty()) {
            findViewById(R.id.device_detail_row_7).setVisibility(View.GONE);
        } else {
            if (!isCollapsed) {
                findViewById(R.id.device_detail_row_7).setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(EXTRA_IS_COLLAPSED, isCollapsed);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        isCollapsed = savedInstanceState.getBoolean(EXTRA_IS_COLLAPSED, false);
        if (!isCollapsed) {
            findViewById(R.id.device_detail_row_1).callOnClick();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * re-init tabs and show first one "general"
     *
     * @param deviceId
     */
    private void initViewPagerAdapter(String deviceId) {
        Log.d(TAG, "init tabs of device detail view");

        if (this.tabLayoutMediator != null) {
            // detach if called twice
            this.tabLayoutMediator.detach();
        }

        viewPagerAdapter = new ViewPager2Adapter(this);
        viewPagerAdapter.setDeviceId(deviceId);
        viewPagerAdapter.setCockpitDbHelper(this.dbHelper);
        viewPagerAdapter.setOpenTabId(this.getIntent().getStringExtra(EXTRA_OPEN_TAB_OID));
        viewPagerAdapter.initTitles(getResources());

        viewPager.setAdapter(viewPagerAdapter);
        TabLayout tl = findViewById(R.id.query_tabs);

        this.tabLayoutMediator = new TabLayoutMediator(tl, viewPager, false,
                (tab, position) -> tab.setText(viewPagerAdapter.getTabTitle(position)));
        this.tabLayoutMediator.attach();

        // FIXME re-implement auto-open tab
//        if (openedTabId != 0) {
//            viewPager.setCurrentItem(staticTabCount + openedTabId, true);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.single_device_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_device_disconnect) {
            if (managedDevice != null) {
                DeviceManager.getInstance()
                        .removeItem(managedDevice.getDeviceConfiguration().getUniqueDeviceId());
                // refresh ui on list change!
                Log.d(TAG, "new device list size: " + DeviceManager.getInstance().getDeviceList().size());
                Toast.makeText(this,
                        String.format(getString(R.string.device_removal_toast),
                                managedDevice.getShortDeviceLabel()),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
            return true;
        }
        if (id == R.id.action_device_refresh) {
            Log.d(TAG, "refreshing cockpit query view");
            // avoid mass double-click
            item.setEnabled(false);
            refreshView(false);
            item.setEnabled(true);
            return true;
        }
        if (id == R.id.action_device_connection_info) {
            Log.d(TAG, "show connection info");

            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_info)
                    .setTitle(R.string.connection_information)
                    .setMessage(managedDevice.getDeviceConfiguration().getConnectionDetailsText())
                    .setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                    .create().show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshView(boolean onlyHead) {
        ProgressBar progressBar = findViewById(R.id.device_progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        if (managedDevice == null || managedDevice.getDeviceConfiguration() == null) {
            Log.w(TAG, "missing device configuration! this should never happen!");
            return;
        }

        if (!onlyHead) {
            CockpitStateManager.getInstance()
                    .getQueryCache().evictDeviceEntries(managedDevice.getDeviceConfiguration().getUniqueDeviceId());
            // update default tabs first
            reloadTabData();
            DeviceDetailFragment ddFragment = (DeviceDetailFragment) viewPagerAdapter.createFragment(0);
            Toast.makeText(this, getString(R.string.device_refresh_toast_message)
                    + ddFragment.getManagedDevice().getLastSystemQuery().getSysName(), Toast.LENGTH_SHORT).show();
        } else {
            CockpitStateManager.getInstance()
                    .getQueryCache().evictSystemQueries();
            progressBar.setVisibility(View.GONE);
        }

        // updating system query information of managed device
        if (!managedDevice.isDummy()) {
            DeviceManager.getInstance().updateSystemQueryAsync(managedDevice);
        }
        updateDeviceInformation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // avoid leaks
        if (this.tabLayoutMediator != null && this.tabLayoutMediator.isAttached()) {
            this.tabLayoutMediator.detach();
        }
        viewPager = null;

        if (alertHelper != null) {
            // avoid window leaks
            alertHelper.closeAllTimeoutAlerts();
        }
        if (periodicTask != null) {
            periodicTask.stop();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * reload all tabs
     * add progress bar finished listener
     */
    private void reloadTabData() {
        Log.d(TAG, "reload tab data");
        // refresh all device fragments with interface method, except first tab
        for (int k = 1; k < viewPagerAdapter.getItemCount(); k++) {
            DeviceFragment fragment = (DeviceFragment) viewPagerAdapter.createFragment(k);
            if (!fragment.isDetached()) {
                if (fragment instanceof DeviceDetailFragment ||
                        fragment instanceof HardwareQueryFragment) {
                    fragment.setOnRenderingFinishedListener(() ->
                            deviceProgressBar.setVisibility(View.GONE));
                }
                fragment.reloadData();
            }
        }
    }

    @Override
    public void restartQueryCall() {
        reloadTabData();
    }

    public void checkSecurity() {
        Log.d(TAG, "periodic security check");
        restartTrigger(this);
    }
}
