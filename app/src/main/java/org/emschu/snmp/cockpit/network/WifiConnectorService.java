package org.emschu.snmp.cockpit.network;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.zxing.client.result.WifiParsedResult;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;

import java.util.List;

public class WifiConnectorService {

    private final static String TAG = WifiConnectorService.class.getSimpleName();
    private final WifiManager wm;
    private final ConnectivityManager cm;


    public WifiConnectorService() {
        Context applicationContext = SnmpCockpitApp.getContext().getApplicationContext();
        this.wm = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        this.cm = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @SuppressWarnings("deprecation")
    public void connect(String ssid) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // "modern" implementation
            WifiParsedResult wpr = new WifiParsedResult("WPA2", ssid, "");
            this.connect(wpr);
        } else {
            // "legacy"/deprecated implementation
            if (ActivityCompat.checkSelfPermission(SnmpCockpitApp.getContext().getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                List<android.net.wifi.WifiConfiguration> wifiConfigurationList = this.wm.getConfiguredNetworks();
                if (wifiConfigurationList == null || wifiConfigurationList.isEmpty()) {
                    return;
                }
                for (android.net.wifi.WifiConfiguration config : wifiConfigurationList) {
                    if (ssid.equals(config.SSID.replace("\"", ""))) {
                        Log.d(TAG, "trying to set target network to active");

                        // notify network stuff to users
                        Toast.makeText(SnmpCockpitApp.getContext().getApplicationContext(),
                                SnmpCockpitApp.getContext().getApplicationContext().getString(R.string.connect_to_device_toast, ssid), Toast.LENGTH_SHORT).show();
                        this.wm.enableNetwork(config.networkId, true);
                        return;
                    }
                }
            }
        }
    }

    public void connect(WifiParsedResult parsedResult) {
        if (wm == null) {
            Log.e(TAG, "No WifiManager available!");
            return;
        }

        List<ScanResult> wifiConfigurationList = wm.getScanResults();
        if (wifiConfigurationList == null || wifiConfigurationList.isEmpty()) {
            return;
        }
        Log.d(TAG, "trying to set target network to active");

        WifiNetworkSpecifier networkSpecifier = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // modern implementation
            networkSpecifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(parsedResult.getSsid())
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(networkSpecifier).build();

            ConnectivityManager.NetworkCallback nc = new ConnectivityManager.NetworkCallback();

            cm.requestNetwork(request, nc, 15000);
        } else {
            // deprecated implementation
            WifiConnector connector = new WifiConnector(parsedResult);
            if (connector.canConnect()) {
                connector.connect(this.wm);

                if (connector.isFailed()) {
                    Toast.makeText(SnmpCockpitApp.getContext().getApplicationContext(),
                            SnmpCockpitApp.getContext().getApplicationContext().getString(R.string.connect_to_device_toast_failed,
                                    parsedResult.getSsid()), Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w(TAG, "invalid network type");
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void suggestWifiEnabled() {
        Context applicationContext = SnmpCockpitApp.getContext().getApplicationContext();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            SnmpCockpitApp.getContext().getApplicationContext().startActivity(panelIntent, new Bundle());
        } else {
            WifiManager wm = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
            wm.setWifiEnabled(true);
            Toast.makeText(applicationContext, R.string.wifi_activated_toast_label, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    public void suggestWifiDisabled() {
        Context applicationContext = SnmpCockpitApp.getContext().getApplicationContext();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            applicationContext.startActivity(panelIntent, new Bundle());
        } else {
            WifiManager wm = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
            wm.setWifiEnabled(false);
            Toast.makeText(SnmpCockpitApp.getContext().getApplicationContext(), R.string.wifi_disable_adapter, Toast.LENGTH_SHORT).show();
        }
    }
}
