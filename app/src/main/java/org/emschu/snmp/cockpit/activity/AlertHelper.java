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
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Process;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.emschu.snmp.cockpit.CockpitMainActivity;
import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.network.WifiConnectorService;
import org.emschu.snmp.cockpit.network.WifiNetworkManager;
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.snmp.ManagedDevice;
import org.emschu.snmp.cockpit.snmp.SnmpManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * this class encapsulates alert dialogs and their functionality in this app
 */
public class AlertHelper {
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 167;
    private final WeakReference<Context> context;

    private final CockpitPreferenceManager cockpitPreferenceManager;
    private final ArrayList<AlertDialog> alertDialogs = new ArrayList<>();
    private final ArrayList<AlertDialog> welcomeDialogs = new ArrayList<>();
    private final ArrayList<AlertDialog> timeoutDialogs = new ArrayList<>();
    private final ArrayList<AlertDialog> sessionTimeoutDialogs = new ArrayList<>();
    private static final String TAG = AlertHelper.class.getName();

    /**
     * constructor
     *
     * @param context
     */
    public AlertHelper(Context context) {
        this.context = new WeakReference<>(context);
        this.cockpitPreferenceManager = new CockpitPreferenceManager(this.context.get());
    }

    /**
     * this dialog displays the flashlight hint dialog and - on dismiss - it starts the wifi qr code scanner
     * given in input params
     *
     * @param qrScannerActivityHelper called on dismiss
     */
    public void showFlashlightHintDialog(QrScannerActivityHelper qrScannerActivityHelper, boolean isWifi) {
        // show alert and start scanner on dismiss event
        new AlertDialog.Builder(context.get())
                .setCancelable(true)
                .setMessage(R.string.flashlight_hint)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> dialog.dismiss())
                .setNegativeButton(R.string.btn_no_longer_show, (dialog, which) -> cockpitPreferenceManager.disableFlashLightHint())
                .setOnDismissListener(dialog -> {
                    if (isWifi) {
                        qrScannerActivityHelper.startWifiScanner();
                    } else {
                        qrScannerActivityHelper.startDeviceScanner();
                    }
                }).show();
    }

    /**
     * wifi not accessible
     *
     * @param wifiSSid
     */
    public void showUnsuccessfulWifiConnectionAlert(String wifiSSid) {
        new AlertDialog.Builder(context.get())
                .setCancelable(true)
                .setMessage(String.format(context.get().getString(R.string.error_connecting_to_network), wifiSSid)).show();
    }

    private void cleanupDialogList(@NonNull List<AlertDialog> dialogList) {
        Iterator<AlertDialog> iterator = dialogList.iterator();
        while(iterator.hasNext()) {
            AlertDialog dialog = iterator.next();
            if (!dialog.isShowing()) {
                iterator.remove();
            }
        }
    }

    /**
     * builds the network security blocking overlay dialog
     * NOTE: only 3 buttons possible
     */
    public void showNotSecureAlert(Activity protectedActivity) {
        cleanupDialogList(alertDialogs);
        if (!alertDialogs.isEmpty()) {
            Log.i(TAG, "security dialog is already shown");
            return;
        }
        if (CockpitStateManager.getInstance().isInTestMode()) {
            Log.i(TAG, "security dialog is not show in test mode");
            return;
        }

        WifiNetworkManager cockpitWifiNetworkManager = WifiNetworkManager.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(protectedActivity)
                .setCancelable(false)
                .setTitle(R.string.no_secure_environment)
                .setMessage(context.get().getString(R.string.no_secure_environment_label)
                        + cockpitWifiNetworkManager.getCurrentModeLabel())
                .setPositiveButton(R.string.menu_settings_label, (dialog, which) -> {
                    alertDialogs.clear();
                    protectedActivity
                            .startActivityForResult(new Intent(context.get(), BlockedSettingsActivity.class), SETTINGS_ACTIVITY_REQUEST_CODE);
                    if (protectedActivity instanceof ProtectedActivity) {
                        ((ProtectedActivity) protectedActivity).restartTrigger(protectedActivity);
                    }
                })
                .setNeutralButton(R.string.menu_action_qr_code_label,
                        (dialog, which) -> new QrScannerActivityHelper((CockpitMainActivity) context.get()).startWifiScanner());

        // if wifi is disabled - we show the button "enable wifi"
        WifiManager androidWifiManager = cockpitWifiNetworkManager.getAndroidWifiManager();
        if (androidWifiManager != null && !androidWifiManager.isWifiEnabled()) {
            builder.setNegativeButton(R.string.menu_action_wifi_activate, (dialog, which) -> {
                if (!androidWifiManager.isWifiEnabled()) {
                    Log.i(TAG, "Enable wifi with app");
                    WifiConnectorService wcs = new WifiConnectorService();
                    wcs.suggestWifiEnabled();
                }
            });
        } else {
            if (cockpitWifiNetworkManager.isTargetNetworkReachable()) {
                // show "connect" button for target fixed ssid
                builder.setNegativeButton(R.string.reconnect_button_label, (dialog, which) -> cockpitWifiNetworkManager.connectToTargetNetwork());
            } else {
                // .. or else: offer close app btn - there is nothing we can do
                builder.setNegativeButton(R.string.cancel_app_btn_label, (dialog, which) -> Process.killProcess(Process.myPid()));
            }
        }
        builder.setIcon(R.drawable.ic_warning_black_48_dp);

        if (context.get() instanceof Activity && !((Activity) context.get()).isFinishing()) {
            ((Activity) context.get()).runOnUiThread(() -> {
                AlertDialog dialog = builder.create();
                alertDialogs.add(dialog);
                dialog.show();
            });
        } else {
            Log.e(TAG, "no valid context!");
        }
    }

    /**
     * helper to close all security alert dialogs at once
     */
    public void closeAllSecurityAlerts() {
        Log.d(TAG, "close all security alerts");
        for (AlertDialog dialog : alertDialogs) {
            dialog.cancel();
        }
        alertDialogs.clear();
    }

    /**
     * method to close all timeout alerts
     */
    public void closeAllTimeoutAlerts() {
        Log.d(TAG, "close all timeout alerts");
        for (AlertDialog dialog : timeoutDialogs) {
            dialog.cancel();
        }
        timeoutDialogs.clear();
        for (AlertDialog dialog : sessionTimeoutDialogs) {
            dialog.cancel();
        }
        sessionTimeoutDialogs.clear();
    }

    /**
     * shows the connection broken dialog
     */
    public synchronized void showDeviceTimeoutDialog() {
        cleanupDialogList(timeoutDialogs);
        if (!timeoutDialogs.isEmpty()) {
            Log.d(TAG, "dialog is already shown");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context.get());

        final ManagedDevice[] devicesInTimeout = SnmpManager.getInstance().getDevicesInTimeout();
        if (devicesInTimeout.length == 0) {
            Log.e(TAG, "no devices in timeout, but dialog was called!");
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean isSingular = devicesInTimeout.length == 1;
        String positiveButtonLabel = context.get().getString(R.string.alert_connection_timeout_multi_remove);
        if (isSingular) {
            sb.append(context.get().getString(R.string.alert_connection_timeout_single_message)).append("\n");
            sb.append(devicesInTimeout[0].getDeviceLabel());
            positiveButtonLabel = context.get().getString(R.string.alert_connection_timeout_single_remove);
        } else {
            sb.append(context.get().getString(R.string.alert_connection_timeout_multiple_devices)).append("\n");
            for (ManagedDevice dc : devicesInTimeout) {
                sb.append("\t• ").append(dc.getDeviceLabel()).append("\n");
            }
        }
        builder.setIcon(R.drawable.ic_warning_black_48_dp);

        builder.setMessage(sb.toString())
                .setCancelable(false)
                .setTitle(R.string.alert_connection_timeout_title)
                .setNeutralButton(R.string.alert_connection_timeout_retry, (dialog, which) -> {
                    Log.d(TAG, "action: retry connections");
                    for (ManagedDevice md : devicesInTimeout) {
                        SnmpManager.getInstance().resetTimeout(md.getDeviceConfiguration());
                    }
                    if (context.get() instanceof ProtectedActivity) {
                        ((ProtectedActivity) context.get()).restartQueryCall();
                    }
                    dialog.cancel();
                })
                .setPositiveButton(positiveButtonLabel, (dialog, which) -> {
                    Log.d(TAG, "action: remove connections");
                    for (ManagedDevice md : devicesInTimeout) {
                        // implicit "timeout reset"
                        DeviceManager.getInstance().removeItem(md.getDeviceConfiguration().getUniqueDeviceId());
                    }
                    // only close
                    if (context.get() instanceof TabbedDeviceActivity
                            || context.get() instanceof SingleQueryResultActivity) {
                        ((Activity) context.get()).finish();
                    } else if (context.get() instanceof Activity) {
                        ((Activity) context.get()).recreate();
                    }
                    dialog.cancel();
                })
                .setNegativeButton(R.string.cancel_app_btn_label, (dialog, which) -> {
                    Log.d(TAG, "action: quit app");
                    Process.killProcess(Process.myPid());
                });

        if (context.get() instanceof Activity && !((Activity) context.get()).isFinishing() && !((Activity) context.get()).isDestroyed()) {
            ((Activity) context.get()).runOnUiThread(() -> {
                AlertDialog dialog = builder.create();
                timeoutDialogs.add(dialog);
                dialog.show();
            });
        }
    }

    /**
     * session timeout dialog
     */
    public void showSessionTimeoutDialog() {
        cleanupDialogList(sessionTimeoutDialogs);
        if (!sessionTimeoutDialogs.isEmpty()) {
            Log.d(TAG, "session timeout dialog is already shown");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context.get());
        builder.setTitle(R.string.alert_session_timeout_title)
                .setMessage(R.string.alert_session_timeout_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.cancel();
                    if (context.get() instanceof CockpitMainActivity) {
                        ((CockpitMainActivity) context.get()).restartView();
                    } else {
                        if (context.get() instanceof Activity) {
                            ((Activity) context.get()).finish();
                        }
                    }
                    if (context.get() instanceof ProtectedActivity) {
                        ((ProtectedActivity) context.get()).restartQueryCall();
                    }
                })
                .setOnDismissListener(dialog -> {
                    // "clear all" and unset session timeout
                    CockpitStateManager.getInstance().getIsInSessionTimeoutObservable().setValueAndTriggerObservers(false);
                    CockpitStateManager.getInstance().getIsInTimeoutsObservable().setValueAndTriggerObservers(false);
                });
        builder.setIcon(R.drawable.ic_warning_black_48_dp);

        if (context.get() instanceof Activity && !((Activity) context.get()).isFinishing()) {
            ((Activity) context.get()).runOnUiThread(() -> {
                AlertDialog alertDialog = builder.create();
                sessionTimeoutDialogs.add(alertDialog);
                alertDialog.show();
            });
        }
    }

    /**
     * show dialog to start an oid query
     *
     * @param oidValue
     */
    public void showQueryTargetDialog(String oidValue) {
        Map<String, String> items = DeviceManager.getInstance().getDisplayableDeviceList();

        if (items.isEmpty()) {
            Log.d(TAG, "do not show dialog - no devices listed");

            new AlertDialog.Builder(context.get())
                    .setMessage(R.string.alert_please_add_device_first)
                    .setCancelable(true)
                    .create().show();
            return;
        }

        String[] deviceIds = new String[items.size()];
        String[] deviceLabels = new String[items.size()];
        int i = 0;
        for (Map.Entry<String, String> deviceItem : items.entrySet()) {
            deviceIds[i] = deviceItem.getKey();
            deviceLabels[i] = deviceItem.getValue();
            i++;
        }
        new AlertDialog.Builder(context.get())
                .setTitle(context.get().getString(R.string.alert_select_device_dialog_title) + " '" + oidValue + "'")
                .setCancelable(true)
                .setSingleChoiceItems(deviceLabels, 0, null)
                .setNeutralButton(R.string.alert_open_query_in_new_device_tab, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Log.d(TAG, "device selected: " + selectedPosition);
                    ManagedDevice md = DeviceManager.getInstance().getDevice(deviceIds[selectedPosition]);
                    dialog.dismiss();
                    if (md != null) {
                        showNewQuery(true, md.getDeviceConfiguration(), oidValue);
                    }
                })
                .setPositiveButton(R.string.alert_open_query_in_new_activity, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Log.d(TAG, "device selected: " + selectedPosition);
                    ManagedDevice md = DeviceManager.getInstance().getDevice(deviceIds[selectedPosition]);
                    dialog.dismiss();
                    if (md != null) {
                        showNewQuery(false, md.getDeviceConfiguration(), oidValue);
                    }
                })
                .setNegativeButton(R.string.close, null)
                .create().show();
    }

    /**
     * method to display a new tab in a single device activity or a new simple query activity
     *
     * @param isNewTab
     * @param deviceConfiguration
     * @param oidQuery
     */
    private void showNewQuery(boolean isNewTab, DeviceConfiguration deviceConfiguration, String oidQuery) {
        Log.d(TAG, "show new query " + oidQuery);
        if (!isNewTab) {
            // show new activity
            Intent deviceDetailIntent = new Intent(context.get(), SingleQueryResultActivity.class);
            deviceDetailIntent.putExtra(SingleQueryResultActivity.EXTRA_DEVICE_ID, deviceConfiguration.getUniqueDeviceId());
            deviceDetailIntent.putExtra(SingleQueryResultActivity.EXTRA_OID_QUERY, oidQuery);
            context.get().startActivity(deviceDetailIntent);
        } else {
            // show default device detail activity
            DeviceManager.getInstance().addNewDeviceTab(deviceConfiguration.getUniqueDeviceId(), oidQuery);
            Intent deviceDetailIntent = new Intent(context.get(), TabbedDeviceActivity.class);
            deviceDetailIntent.putExtra(TabbedDeviceActivity.EXTRA_DEVICE_ID, deviceConfiguration.getUniqueDeviceId());
            deviceDetailIntent.putExtra(TabbedDeviceActivity.EXTRA_OPEN_TAB_OID, oidQuery);
            context.get().startActivity(deviceDetailIntent);
        }
    }

    /**
     * confirmation dialog before connection test is closed
     */
    public void showCancelConnectionConfirmationDialog() {
        new AlertDialog.Builder(context.get()).setTitle(R.string.dialog_cancel_connection_dialog_title)
                .setMessage(R.string.dialog_cancel_connection_test_message)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    Log.d(TAG, "cancel connection attempt");
                    if (context.get() instanceof CockpitMainActivity) {
                        ((CockpitMainActivity) context.get()).cancelConnectionTestTask();
                        ((CockpitMainActivity) context.get()).getProgressRow().setVisibility(View.GONE);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    Log.d(TAG, "NOT canceling connection attempt");
                    dialog.dismiss();
                })
                .create().show();
    }

    public void showWelcomeAlert(WifiNetworkManager wifiNetworkManager) {
        cleanupDialogList(welcomeDialogs);
        if (!welcomeDialogs.isEmpty()) {
            Log.i(TAG, "welcome dialog is already shown");
            return;
        }
        if (CockpitStateManager.getInstance().isInTestMode()) {
            Log.i(TAG, "security dialog is not show in test mode");
            return;
        }
        
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context.get())
                .setTitle(R.string.welcome_dialog_title)
                .setMessage(R.string.welcome_dialog_message)
                .setIcon(R.drawable.ic_info_black)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    Log.d(TAG, "Welcome screen finished");
                    cockpitPreferenceManager.setWelcomeScreenShown();
                    dialog.dismiss();
                });

        if (wifiNetworkManager.hasWifiConnection()) {
            dialogBuilder.setNeutralButton(R.string.welcome_dialog_take_current_ssid_btn, (dialog, which) -> {
                String ssid = wifiNetworkManager.getCurrentSsid();
                Log.d(TAG, String.format("Take current SSID for as secure ssid '%s'", ssid));
                cockpitPreferenceManager.updateFixedSSID(ssid);
                cockpitPreferenceManager.setWelcomeScreenShown();
                dialog.dismiss();
            });
        }

        AlertDialog welcomeDialog = dialogBuilder
                .create();

        welcomeDialog.show();
        welcomeDialogs.add(welcomeDialog);
    }

    public void showPermissionLocationDialog(@NonNull SimpleDialogResult simpleDialogResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context.get())
                .setTitle(R.string.permission_required)
                .setMessage(R.string.permission_location_info_text)
                .setIcon(R.drawable.ic_info_black)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    Log.d(TAG, "Permission screen finished");
                    simpleDialogResult.setResult(true);
                    simpleDialogResult.onApproval();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                    Log.d(TAG, "Permission screen cancelled");
                    simpleDialogResult.setResult(false);
                    simpleDialogResult.onDenial();
                    dialog.dismiss();
                }));
        builder.create().show();
    }

    /**
     * class to handle simple dialog events. Supports "Yes" and "No".
     */
    public static abstract class SimpleDialogResult {
        private boolean result = false;

        // callbacks
        public abstract void onApproval();
        public abstract void onDenial();

        public boolean isSuccess() {
            return result;
        }
        private void setResult(boolean result) {
            this.result = result;
        }
    }
}
