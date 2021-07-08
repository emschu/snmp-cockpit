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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.WifiParsedResult;
import com.google.zxing.client.result.WifiResultParser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.emschu.snmp.cockpit.activity.AlertHelper;
import org.emschu.snmp.cockpit.activity.ProtectedActivity;
import org.emschu.snmp.cockpit.activity.QrScannerActivityHelper;
import org.emschu.snmp.cockpit.activity.SNMPLoginActivity;
import org.emschu.snmp.cockpit.activity.TabbedDeviceActivity;
import org.emschu.snmp.cockpit.fragment.AboutFragment;
import org.emschu.snmp.cockpit.fragment.DeviceMonitorViewFragment;
import org.emschu.snmp.cockpit.fragment.MibCatalogFragment;
import org.emschu.snmp.cockpit.fragment.NetworkDetailsFragment;
import org.emschu.snmp.cockpit.fragment.OwnQueryFragment;
import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent;
import org.emschu.snmp.cockpit.network.WifiNetworkManager;
import org.emschu.snmp.cockpit.query.OIDCatalog;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.MibCatalogManager;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.emschu.snmp.cockpit.tasks.DeviceAddTask;
import org.emschu.snmp.cockpit.util.BooleanObservable;
import org.emschu.snmp.cockpit.util.PeriodicTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * main activity of this app
 * <p>
 * atm these 5 screens are displayed ({@link CockpitScreens}):
 * - main screen
 * - custom query screen
 * - oid catalog screen
 * - settings screen
 * - about screen
 */
public class CockpitMainActivity extends ProtectedActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DeviceMonitorViewFragment.OnListFragmentInteractionListener {

    private static final String TAG = CockpitMainActivity.class.getName();
    private static final String KEY_LAST_SCREEN = "last_screen";

    public static final String QUERY_OWN_CATALOG_FRAGMENT_TAG = "ownQueryFragment";
    private static final String CONNECTION_TEST_TASK_TAG = "connection_test_task";
    private final CockpitStateManager cockpitStateManagerInstance = CockpitStateManager.getInstance();
    private BooleanObservable booleanObservable = null;

    // floating action buttons
    private FloatingActionButton addDeviceMainFab = null;
    private TextView noDeviceText;

    // fragments
    private final DeviceMonitorViewFragment deviceMonitorViewFragment = DeviceMonitorViewFragment.newInstance();
    private final MibCatalogFragment mibCatalogFragment = MibCatalogFragment.newInstance();
    private OwnQueryFragment ownQueryFragment = OwnQueryFragment.newInstance();
    private final NetworkDetailsFragment networkDetailsFragment = NetworkDetailsFragment.newInstance();
    private ProgressBar progressBar;
    private CockpitScreens screenBefore = null;
    private CockpitScreens lastScreen = CockpitScreens.SCREEN_MAIN_DEVICES;

    // services/managers
    private WifiNetworkManager wifiNetworkManager = null;

    private QrScannerActivityHelper qrScannerActivityHelper;
    private CockpitPreferenceManager cockpitPreferenceManager;
    private AlertHelper alertHelper;
    private LinearLayout progressRow;
    private PeriodicTask periodicTask;
    private NavigationView navigationView;
    private boolean backPressState;
    private Long lastStateCheckAt = null;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Log.d(TAG, "Permission granted!");
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Log.e(TAG, "Permission missing!");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // configure very strict policies in debug mode to get bugs pop up early
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detectNonSdkApiUsage()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            System.setErr(new PrintStreamThatDumpsHprofWhenStrictModeKillsUs(System.err));
        }

        if (savedInstanceState != null) {
            OwnQueryFragment fragment = (OwnQueryFragment)
                    getSupportFragmentManager().getFragment(savedInstanceState, QUERY_OWN_CATALOG_FRAGMENT_TAG);
            if (fragment != null) {
                ownQueryFragment = fragment;
            }
        }
        // init oid catalog async
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            Log.d(TAG, "start oid catalog init");
            OIDCatalog.getInstance(new MibCatalogManager(PreferenceManager.getDefaultSharedPreferences(this)));
            Log.d(TAG, "finished oid catalog init");
        });
        executorService.shutdown();

        booleanObservable = cockpitStateManagerInstance.getNetworkSecurityObservable();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        setupFloatingActionButtons();

        alertHelper = new AlertHelper(this);
        cockpitPreferenceManager = SnmpCockpitApp.getPreferenceManager();
        SnmpManager.getInstance().setPreferenceManager(cockpitPreferenceManager);
        qrScannerActivityHelper = new QrScannerActivityHelper(this);
        // instantiate services which needs context
        wifiNetworkManager = WifiNetworkManager.getInstance();

        OnSecurityStateChangeListener listener = new OnSecurityStateChangeListener() {
            @Override
            public void onInsecureState() {
                cancelConnectionTestTask();
                progressRow.setVisibility(View.GONE);
            }

            @Override
            public void onSecureState() { // not needed
            }
        };

        // ensure only the following one observer is listening
        initObservables(alertHelper, listener);

        // set progress bar gone after init actions done
        if (progressBar == null) {
            progressBar = findViewById(R.id.app_progress_bar);
            progressRow = findViewById(R.id.progress_info_row);
            if (cockpitStateManagerInstance.isConnecting()) {
                progressRow.setVisibility(View.VISIBLE);
            } else {
                progressRow.setVisibility(View.GONE);
            }

            ImageButton cancelConnectionTestBtn = findViewById(R.id.cancel_connection_attempt_button);
            cancelConnectionTestBtn.setOnClickListener(v -> alertHelper.showCancelConnectionConfirmationDialog());
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(new NavigationDrawerListener());
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // initial set of query fragments
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, deviceMonitorViewFragment)
                .commit();
        navigationView.getMenu().getItem(0).setChecked(true);
        // set welcome screen
        noDeviceText = findViewById(R.id.textview_noDeviceText);
        noDeviceText.setText(R.string.empty_device_list_message);
        setMainScreenVisible();
        initTasks();

        // show initial welcome screen
        if (!cockpitPreferenceManager.isWelcomeScreenShown()) {
            alertHelper.showWelcomeAlert(wifiNetworkManager);
        }

        // check for permissions
        handlePermissions();

        // handle display of "no-device"-text
        BooleanObservable areDevicesConnectedObservable = CockpitStateManager.getInstance().getAreDevicesConnectedObservable();
        areDevicesConnectedObservable.deleteObservers(); // ensure only one observer is registered
        areDevicesConnectedObservable.addObserver((o, arg) -> {
            runOnUiThread(() -> {
                boolean areDevicesConnected = ((BooleanObservable) o).getValue();
                if (areDevicesConnected) {
                    noDeviceText.setVisibility(View.INVISIBLE);
                } else {
                    noDeviceText.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void handlePermissions() {
        String[] requiredPermissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
        };

        for (String permission : requiredPermissions) {
            boolean permissionCheck = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            if (permissionCheck) {
                // continue if permission is granted already
                continue;
            }
            if (shouldShowRequestPermissionRationale(permission)) {
                this.alertHelper.showPermissionLocationDialog(new AlertHelper.SimpleDialogResult() {
                    @Override
                    public void onApproval() {
                        requestPermissionLauncher.launch(permission);
                    }

                    @Override
                    public void onDenial() {
                        Toast.makeText(getApplicationContext(), "Missing required permissions!", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                this.requestPermissionLauncher.launch(permission);
            }
        }
    }

    /**
     * init periodic tasks
     */
    public void initTasks() {
        if (periodicTask != null) {
            periodicTask.stop();
        }
        if (new CockpitPreferenceManager(this).isPeriodicUpdateEnabled()) {
            periodicTask = new PeriodicTask(this::restartQueryCall,
                    cockpitPreferenceManager.getUiUpdateSeconds() * 1000);
        } else {
            periodicTask = new PeriodicTask(this::checkState, 2500);
        }
        checkState();
    }

    /**
     * helper method to cancel running connection tests
     */
    public void cancelConnectionTestTask() {
        Log.d(TAG, "request to cancel all connection test tasks");

        WorkManager.getInstance(this).cancelAllWorkByTag(CONNECTION_TEST_TASK_TAG);
        cockpitStateManagerInstance.setConnecting(false);

        // set default text
        TextView infoTextView = findViewById(R.id.connection_attempt_count_label);
        infoTextView.setText(getString(R.string.connection_attempt_label));
    }

    /**
     * this is called in {@link #onCreate(Bundle)}
     */
    private void setupFloatingActionButtons() {
        addDeviceMainFab = findViewById(R.id.fab_add_device);

        ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        startConnectionTest(result);
                    }
                });

        addDeviceMainFab.setOnClickListener(view -> {
            this.openLoginActivity(activityLauncher);
        });
    }

    private void startConnectionTest(androidx.activity.result.ActivityResult result) {
        Intent data = result.getData();
        DeviceManager deviceManager = DeviceManager.getInstance();
        DeviceConfiguration config = deviceManager.createDeviceConfiguration(data);

        // apply snmp version user pref
        if (config.getSnmpVersion() < 3) {
            if (cockpitPreferenceManager.isV1InsteadOfV3()) {
                config.setSnmpVersion(DeviceConfiguration.SNMP_VERSION.v1);
            } else {
                config.setSnmpVersion(DeviceConfiguration.SNMP_VERSION.v2c);
            }
        }

        config.setTimeout(cockpitPreferenceManager.getConnectionTimeout());
        config.setRetries(cockpitPreferenceManager.getConnectionRetries());
        Log.d(TAG, "set user defined connection timeout to "
                + config.getTimeout() + " with " + config.getRetries() + " retries");

        Log.i(TAG, "start connectivity check task and add device to list - if check does not fail");
        progressRow.setVisibility(View.VISIBLE);
        cockpitStateManagerInstance.setConnecting(true);
        cockpitStateManagerInstance.setTestConfiguration(config);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DeviceAddTask.class)
                .setConstraints(constraints).addTag(CONNECTION_TEST_TASK_TAG).build();

        WorkManager instance = WorkManager.getInstance(this);
        instance.enqueueUniqueWork("connection_test_work", ExistingWorkPolicy.KEEP, workRequest);
        instance.getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        cockpitStateManagerInstance.setTestConfiguration(null);
                        // handle success/error/cancel cases
                        if (workInfo.getState() == WorkInfo.State.FAILED || workInfo.getState() == WorkInfo.State.CANCELLED) {
                            Log.e(TAG, "Connection test NOT successful! Could not connect!");
                            if (workInfo.getOutputData().hasKeyWithValueOfType(DeviceAddTask.OUTPUT_DOES_EXIST, Boolean.class)) {
                                Toast.makeText(this, R.string.connection_already_exists, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, R.string.connection_test_not_successful_label, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.i(TAG, "Connection test successful");
                            Toast.makeText(this, getString(R.string.connection_test_successful_label), Toast.LENGTH_SHORT).show();

                            DeviceManager.getInstance().add(config, false);
                            // refresh ui on list change!
                            RecyclerView.Adapter<?> adapter = deviceMonitorViewFragment.getRecyclerView().getAdapter();
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            checkNoData();
                        }
                        progressRow.setVisibility(View.GONE);
                        TextView infoTextView = this.progressRow.findViewById(R.id.connection_attempt_count_label);
                        infoTextView.setText(SnmpCockpitApp.getContext().getString(R.string.connection_attempt_label));
                        CockpitStateManager.getInstance().setConnecting(false);
                    } else {
                        // handle progress update
                        if (workInfo != null) {
                            Data progressData = workInfo.getProgress();
                            if (progressData.hasKeyWithValueOfType(DeviceAddTask.PROGRESS_CURRENT_NUM, Integer.class)) {
                                int allProgress = progressData.getInt(DeviceAddTask.PROGRESS_ALL_NUM, 0);
                                int currentProgress = progressData.getInt(DeviceAddTask.PROGRESS_CURRENT_NUM, 0);

                                TextView infoTextView = this.progressRow.findViewById(R.id.connection_attempt_count_label);
                                infoTextView.setText(String.format(getCurrentLocale(), "%s %d/%d",
                                        SnmpCockpitApp.getContext().getString(R.string.connection_attempt_label),
                                        currentProgress, allProgress)
                                );
                            }
                        }
                    }
                });
    }

    @SuppressWarnings({"deprecation"})
    private Locale getCurrentLocale() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            return this.getResources().getConfiguration().locale;
        }
        return this.getResources().getConfiguration().getLocales().get(0);
    }

    private void openLoginActivity(ActivityResultLauncher<Intent> activityLauncher) {
        addDeviceMainFab.setEnabled(false);
        System.gc();
        Intent intent = new Intent(this, SNMPLoginActivity.class);
        activityLauncher.launch(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        // first effect: close drawer
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }

        if (screenBefore != null) {
            showScreen(screenBefore);
            // this is not repeatable
            if (screenBefore == CockpitScreens.SCREEN_MAIN_DEVICES) {
                screenBefore = null;
            } else {
                screenBefore = CockpitScreens.SCREEN_MAIN_DEVICES;
            }
            return;
        }
        if (backPressState) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        backPressState = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> backPressState = false, 500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkState();
        startProtection(this);
        periodicTask.start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restartView();
        updateNetworkInformation();
        initTasks();
        checkState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        periodicTask.stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (periodicTask != null) {
            periodicTask.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initTasks();
        periodicTask.start();
        checkState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProtection(this);
        if (periodicTask != null) {
            periodicTask.stop();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastScreen != null) {
            outState.putInt(KEY_LAST_SCREEN, lastScreen.ordinal());
        }

        if (ownQueryFragment != null && ownQueryFragment.isAdded()) {
            getSupportFragmentManager()
                    .putFragment(outState, QUERY_OWN_CATALOG_FRAGMENT_TAG, ownQueryFragment);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "on restore");
        int lastDisplayedScreen = savedInstanceState.getInt(KEY_LAST_SCREEN, -1);
        if (lastDisplayedScreen != -1) {
            Log.d(TAG, "show screen " + lastDisplayedScreen);
            showScreen(CockpitScreens.values()[lastDisplayedScreen]);
        }
        updateNetworkInformation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            item.setEnabled(false);
            refreshView();
            item.setEnabled(true);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * refresh ui and display some log debug information
     */
    private void refreshView() {
        runOnUiThread(() -> {
            progressRow.findViewById(R.id.app_progress_bar).setVisibility(View.VISIBLE);
            cockpitStateManagerInstance.getQueryCache().evictSystemQueries();
            checkState();
            checkNoData();
            updateNetworkInformation();
        });
        if (!DeviceManager.getInstance().getDeviceList().isEmpty() && deviceMonitorViewFragment != null) {
            deviceMonitorViewFragment.refreshListAsync();
            // only show toast if there are entries..
            Log.d(TAG, "update adapter");
            RecyclerView.Adapter<?> adapter = deviceMonitorViewFragment.getRecyclerView().getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        Log.d(TAG, "current device list: " + DeviceManager.getInstance().getDeviceList());
    }

    /**
     * method to update diplayed network information
     */
    private void updateNetworkInformation() {
        Log.d(TAG, "update network information");
        wifiNetworkManager.refresh(); // refreshes dhcp info
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        TextView ipLabel = drawer.findViewById(R.id.nav_header_ip_textview);
        if (ipLabel == null) {
            Log.d(TAG, "no drawer view inited!");
            return;
        }
        ipLabel.setText(wifiNetworkManager.getIpAddressLabel());

        TextView ssidField = drawer.findViewById(R.id.nav_header_ssid_textview);
        ssidField.setText(wifiNetworkManager.getCurrentSSIDLabel());

        TextView ipv6Field = drawer.findViewById(R.id.nav_header_ipv6_textview);
        ipv6Field.setText(wifiNetworkManager.getIpv6AddressLabel());
    }

    /**
     * checks current network state and shows alert
     */
    public void checkState() {
        // prevent mass calling of state checks
        long minimalIntervalBetweenChecks = DeviceManager.getInstance().getDeviceList().size() == 0 ? 2500L : 500L;
        if (this.lastStateCheckAt != null && System.currentTimeMillis() - this.lastStateCheckAt < minimalIntervalBetweenChecks) {
            return;
        }
        this.lastStateCheckAt = System.currentTimeMillis();

        boolean networkSecure = wifiNetworkManager.isNetworkSecure();
        Log.d(TAG, "isNetworkSecure: " + networkSecure);
        booleanObservable.setValueAndTriggerObservers(networkSecure);

        if (!networkSecure) {
            Log.d(TAG, "show not secure alert");
            alertHelper.showNotSecureAlert(this);
        }
    }

    /**
     * method to create a screen view state of this activity
     *
     * @param screen
     */
    private void showScreen(CockpitScreens screen) {
        if (screen == null) {
            Log.e(TAG, "Cannot show null screen");
            return;
        }

        // you have to set fab visible explicitly
        addDeviceMainFab.hide();
        // you have to set text visible explicitly
        noDeviceText.setVisibility(View.INVISIBLE);

        navigationView.setCheckedItem(0);
        Log.d(TAG, "showing screen: " + screen);
        switch (screen) {
            case SCREEN_MAIN_DEVICES:
                showMainScreen();
                break;
            case SCREEN_ABOUT:
                showAboutScreen();
                break;
            case SCREEN_SETTINGS:
                showSettingsScreen();
                break;
            case SCREEN_OID_CATALOG:
                showOidCatalogScreen();
                break;
            case SCREEN_OWN_OID_CATALOG:
                showOwnOidCatalogScreen();
                break;
            case SCREEN_NETWORK_DETAILS:
                showNetworkDetailsScreen();
                break;
            default:
                Log.e(TAG, "Invalid screen + " + screen);
                return;
        }

        if (lastScreen == CockpitScreens.SCREEN_SETTINGS) {
            // update tasks - perhaps settings changed
            initTasks();
            onResume();
        }

        screenBefore = lastScreen;
        lastScreen = screen;
        checkNoData();
    }

    /**
     * Note: only display (main) floating action in device monitor screen
     *
     * @param item
     * @return
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // you have to set fab visible explicitly
        addDeviceMainFab.show();
        // you have to set text visible explicitly
        noDeviceText.setVisibility(View.INVISIBLE);

        CockpitScreens cockpitScreen = getCurrentCockpitScreen(id);

        if (id == R.id.nav_scan_wlan_code) {
            // scan a wifi code
            if (cockpitPreferenceManager.showFlashlightHint()) {
                // scanner is started after dialog dismiss
                alertHelper.showFlashlightHintDialog(qrScannerActivityHelper, true);
            } else {
                // show scanner only
                qrScannerActivityHelper.startWifiScanner();
            }
        } else if (id == R.id.nav_clear_all_devices) {
            // TODO use progress bar + async
            DeviceManager.getInstance().removeAllItems();
            onRestart();
            Snackbar.make(findViewById(R.id.app_coordinator), R.string.disconnect_all_devices_success, Snackbar.LENGTH_LONG)
                    .setAction(R.string.disconnect_all_devices_success, null).show();
        } else if (id == R.id.nav_disconnect_wifi) {
            wifiNetworkManager.disconnectWifi();
            Toast.makeText(this, R.string.wifi_disable_adapter, Toast.LENGTH_SHORT).show();
        }

        if (cockpitScreen != null) {
            // screen change detected
            showScreen(cockpitScreen);
        }

        // always show floating action button on main screen
        if (lastScreen == CockpitScreens.SCREEN_MAIN_DEVICES
                && addDeviceMainFab.getVisibility() != View.VISIBLE) {
            setMainScreenVisible();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private CockpitScreens getCurrentCockpitScreen(int id) {
        CockpitScreens cockpitScreen = null;
        if (id == R.id.nav_mib_catalog) {
            cockpitScreen = CockpitScreens.SCREEN_OID_CATALOG;
        } else if (id == R.id.nav_manage_own_queries) {
            cockpitScreen = CockpitScreens.SCREEN_OWN_OID_CATALOG;
        } else if (id == R.id.nav_main_monitoring_view) {
            cockpitScreen = CockpitScreens.SCREEN_MAIN_DEVICES;
        } else if (id == R.id.nav_show_network_details) {
            cockpitScreen = CockpitScreens.SCREEN_NETWORK_DETAILS;
        } else if (id == R.id.nav_settings) {
            cockpitScreen = CockpitScreens.SCREEN_SETTINGS;
        } else if (id == R.id.nav_about) {
            cockpitScreen = CockpitScreens.SCREEN_ABOUT;
        }
        return cockpitScreen;
    }

    /**
     * when a device item was clicked
     *
     * @param item
     */
    @Override
    public void onListFragmentInteraction(DeviceMonitorItemContent.DeviceMonitorItem item) {
        if (CockpitStateManager.getInstance().isInRemoval()) {
            Log.w(TAG, "cannot open device tab during removal event");
            return;
        }
        // avoid InstanceCountViolation
        if (BuildConfig.DEBUG) {
            System.gc();
        }

        Intent deviceDetailIntent = new Intent(this, TabbedDeviceActivity.class);
        deviceDetailIntent.putExtra(TabbedDeviceActivity.EXTRA_DEVICE_ID, item.deviceConfiguration.getUniqueDeviceId());
        startActivity(deviceDetailIntent);
    }

    /**
     * catching results here.
     * result 1: wifi qr code
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this cares about refreshing security dialog after network changes in BlockedSettingsActivity
        if (requestCode == AlertHelper.SETTINGS_ACTIVITY_REQUEST_CODE) {
            // ensure new dialog
            alertHelper.closeAllSecurityAlerts();
            checkState();
            return;
        }
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null && data != null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (result != null && result.getContents() != null) {
            handleWifiQrCode(result);
        }
    }

    /**
     * helper method to handle a wifi qr code
     *
     * @param result
     */
    private void handleWifiQrCode(IntentResult result) {
        boolean isError = false;
        Result wifiResult =
                new Result(result.getContents(), result.getRawBytes(), null,
                        BarcodeFormat.QR_CODE);
        WifiParsedResult parsedResult = new WifiResultParser().parse(wifiResult);
        if (parsedResult != null) {
            String targetSsid = parsedResult.getSsid();
            if (targetSsid == null || targetSsid.isEmpty()) {
                isError = true;
            }
            boolean connectSuccess = wifiNetworkManager.connectNetwork(parsedResult);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "check security after wifi connection attempt");
                checkState();
            }, 2500);
            if (connectSuccess) {
                Toast.makeText(this, String.format(
                        getString(R.string.wifi_qr_code_network_change_success),
                        targetSsid), Toast.LENGTH_LONG).show();
            } else {
                alertHelper.showUnsuccessfulWifiConnectionAlert(targetSsid);
            }
        } else {
            isError = true;
        }
        if (isError) {
            // error
            Toast.makeText(this, R.string.invalid_wifi_qr_code_label, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * method to show settings fragment
     */
    public void showSettingsScreen() {
        setTitle(getResources().getString(R.string.title_activity_settings));
        navigationView.setCheckedItem(R.id.nav_settings);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        CockpitPreferenceManager.GeneralPreferenceFragment generalPreferenceFragment = new CockpitPreferenceManager.GeneralPreferenceFragment();
        transaction.replace(R.id.fragment_container, generalPreferenceFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * method to show oid catalog screen
     */
    public void showOidCatalogScreen() {
        setTitle(getResources().getString(R.string.menu_catalog_label));
        navigationView.setCheckedItem(R.id.nav_mib_catalog);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, mibCatalogFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void showOwnOidCatalogScreen() {
        setTitle(getString(R.string.title_activity_own_queries));
        navigationView.setCheckedItem(R.id.nav_manage_own_queries);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, ownQueryFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void showNetworkDetailsScreen() {
        setTitle(getString(R.string.title_network_details));
        navigationView.setCheckedItem(R.id.nav_show_network_details);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, networkDetailsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * method to show main sreen
     */
    public void showMainScreen() {
        setTitle(getResources().getString(R.string.app_name));
        navigationView.setCheckedItem(R.id.nav_main_monitoring_view);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, deviceMonitorViewFragment);
        transaction.addToBackStack(null);
        transaction.commit();
        setMainScreenVisible();
    }

    /**
     * method to show about screen
     */
    public void showAboutScreen() {
        setTitle(getResources().getString(R.string.title_activity_about));
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new AboutFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public OwnQueryFragment getOwnQueryFragment() {
        return ownQueryFragment;
    }

    //Setting Visibility of the MainScreen
    private void setMainScreenVisible() {
        checkNoData();
        addDeviceMainFab.show();
    }

    /**
     * check if no data = no devices and show message
     */
    public void checkNoData() {
        if (lastScreen == CockpitScreens.SCREEN_MAIN_DEVICES
                && DeviceManager.getInstance().getDeviceList().isEmpty()) {
            noDeviceText.setVisibility(View.VISIBLE);
        } else {
            noDeviceText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void restartQueryCall() {
        refreshView();
    }

    public LinearLayout getProgressRow() {
        return progressRow;
    }

    /**
     * is called after view refreshes
     */
    public void restartView() {
        addDeviceMainFab.setEnabled(true);
        Log.d(TAG, "on restart");
        if (deviceMonitorViewFragment != null) {
            Log.d(TAG, "update adapter");
            RecyclerView.Adapter adapter = deviceMonitorViewFragment.getRecyclerView().getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        restartTrigger(this);
        checkNoData();
    }

    /**
     * enum of all screens we can show.
     * is used to store state of main activity
     */
    public enum CockpitScreens {
        SCREEN_MAIN_DEVICES, SCREEN_OID_CATALOG, SCREEN_OWN_OID_CATALOG, SCREEN_NETWORK_DETAILS, SCREEN_SETTINGS, SCREEN_ABOUT
    }

    /**
     * this class handles navigation drawer actions
     */
    public class NavigationDrawerListener implements DrawerLayout.DrawerListener {

        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            // do nothing
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            TextView counter = drawerView.findViewById(R.id.device_menu_counter_textview);
            counter.setText(String.valueOf(DeviceManager.getInstance().getDeviceList().size()));
            updateNetworkInformation();
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            checkState();
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            // do nothing
        }
    }

    // helper class which is used in debug mode to create a java heap dump if an android policy is violated
    // original source: http://www.codeka.com.au/blog/2014/06/detecting-leaked-activities-in-android
    private static class PrintStreamThatDumpsHprofWhenStrictModeKillsUs extends PrintStream {
        public PrintStreamThatDumpsHprofWhenStrictModeKillsUs(OutputStream outs) {
            super(outs);
        }

        @Override
        public synchronized void println(String str) {
            super.println(str);
            if (str.startsWith("StrictMode VmPolicy violation with POLICY_DEATH;")) {
                // StrictMode is about to terminate us... do a heap dump!
                try {
                    File dir = SnmpCockpitApp.getContext().getExternalFilesDir(null);
                    File file = new File(dir, "strictmode-violation.hprof");
                    super.println("Dumping HPROF to: " + file);
                    Debug.dumpHprofData(file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}