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

import android.content.Intent;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.CockpitStateManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.network.WifiNetworkManager;
import org.emschu.snmp.cockpit.snmp.SnmpEndpoint;
import org.emschu.snmp.cockpit.snmp.json.DeviceQrCode;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * SNMP Login Activity
 * <p>
 * A login screen that offers login via email/password.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SNMPLoginActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        ProtectedActivity {
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String COMMUNITY_KEY = "v1_community";
    public static final String USER_KEY = "v3_user";
    public static final String IPV6_KEY = "is_ipv6";
    @SuppressWarnings("squid:S2068")
    public static final String USER_PASSPHRASE_KEY = "v3_password";
    public static final String ENC_KEY = "v3_enc";
    // default values
    public static final int DEFAULT_SNMP_PORT = 161;
    public static final String DEFAULT_SNMP_COMUNITY = "public";
    private String qrString;

    Spinner snmpSpinner;
    EditText portField; //Port
    EditText communityField; //Community
    EditText hostField; //Host
    EditText userField; //Username
    EditText passwordField; //Password
    EditText encryptField; //Encryption
    SwitchMaterial ipv6EnabledSwitch;

    private static final String TAG = SNMPLoginActivity.class.getName();

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z");

    private static final Pattern PORT_PATTERN =
            Pattern.compile("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");

    // got from here: https://stackoverflow.com/a/17871737
    private static final Pattern IPV6_PATTERN =
            Pattern.compile("(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))");

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snmplogin);
        setupActionBar();

        //EditText which the user or the QR-Scanner fills
        hostField = findViewById(R.id.editTextHost);
        portField = findViewById(R.id.editTextPort);
        communityField = findViewById(R.id.editTextCommunity);
        userField = findViewById(R.id.editTextUsername);
        passwordField = findViewById(R.id.editTextPassword);
        encryptField = findViewById(R.id.editTextEncrypt);

        //Spinner and all the functions
        snmpSpinner = findViewById(R.id.snmp_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.snmp_versions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snmpSpinner.setAdapter(adapter);
        snmpSpinner.setOnItemSelectedListener(this);

        // get default host ips for ipv4 and v6
        final String currentNetworkAddrV4;
        if (WifiNetworkManager.getInstance().getNetInfoProvider() != null) {
            String[] ipv4AddressesRaw = WifiNetworkManager.getInstance().getNetInfoProvider().getIpv4AddressesRaw();
            if (ipv4AddressesRaw != null && ipv4AddressesRaw.length > 0) {
                currentNetworkAddrV4 = ipv4AddressesRaw[0];
            } else {
                currentNetworkAddrV4 = "";
            }
        } else {
            currentNetworkAddrV4 = "";
        }
        final String currentNetworkAddrV6;
        if (WifiNetworkManager.getInstance().getNetInfoProvider() != null) {
            String[] ipv6AddressesRaw = WifiNetworkManager.getInstance().getNetInfoProvider().getIpv6AddressesRaw();
            if (ipv6AddressesRaw != null && ipv6AddressesRaw.length > 0) {
                if (ipv6AddressesRaw[0].contains("%")) {
                    currentNetworkAddrV6 = ipv6AddressesRaw[0].substring(0, ipv6AddressesRaw[0].indexOf("%"));
                } else {
                    currentNetworkAddrV6 = ipv6AddressesRaw[0];
                }
            } else {
                currentNetworkAddrV6 = "";
            }
        } else {
            currentNetworkAddrV6 = "";
        }

        hostField.setText(currentNetworkAddrV4.replace("-", ""));
        ipv6EnabledSwitch = findViewById(R.id.is_ipv6_enabled);
        ipv6EnabledSwitch.setOnClickListener(v -> {
            Log.d(TAG, "change host field input mode");
            if (ipv6EnabledSwitch.isChecked()) {
                // enable ip v6 input mode
                hostField.setKeyListener(TextKeyListener.getInstance());
                hostField.setText(currentNetworkAddrV6);
            } else {
                // enable ip v4 input mode (= default)
                hostField.setKeyListener(DigitsKeyListener.getInstance("1234567890."));
                // replace char for empty "-"
                hostField.setText(currentNetworkAddrV4);
            }
        });
        initObservables(this, new AlertHelper(this), null);

        /*
         * By pressing the addDevice Button, it opens the class ConnectDevice.
         * The ConnectDevice has a SNMP4J library, which connects the device.
         * Also provides all the information (not decided on which typ, maybe as a String) to the class,
         * which the class recalls and connects with. For that just delete the commentent lines.
         */

        // NOTE: do not commit the following lines UNCOMMENTED!

        // use the following lines for debugging
//        snmpSpinner.setSelection(1);
//        portField.setText("162");
//        hostField.setText("10.10.10.221");
//        userField.setText("batmanuser");
//        passwordField.setText("batmankey3");
//        encryptField.setText("batmankey3");
    }

    /**
     * helper method to check if connection can be started and how
     */
    private void onConnect() {
        //If the input is correct, try to connect to the device
        if (isInputValid()) {
            if (CockpitStateManager.getInstance().isConnecting()) {
                Log.d(TAG, "avoid multiple connection attempts at the same time.");

                new AlertDialog.Builder(SNMPLoginActivity.this)
                        .setTitle(getString(R.string.connection_attempt_is_running))
                        .setMessage(getString(R.string.connection_attempt_is_running_msg))
                        .create().show();
                return;
            }
            while (CockpitStateManager.getInstance().isInRemoval()) {
                Log.d(TAG, "wait for removal event is finished");
            }
            //Check first the connection to the device, before you add the device and go back to the main activity.
            Intent data = new Intent();
            data.putExtra(HOST_KEY, hostField.getEditableText().toString());
            // handle default values for port + community (v1)
            String inputPort = portField.getEditableText().toString();
            if (inputPort.isEmpty() || inputPort.equals("null")) {
                inputPort = "" + DEFAULT_SNMP_PORT;
            }
            data.putExtra(PORT_KEY, inputPort);

            // set if ipv4 or ipv6
            data.putExtra(IPV6_KEY, ipv6EnabledSwitch.isChecked());

            if (snmpSpinner.getSelectedItemPosition() == 0) {
                // v1
                String inputCommunity = communityField.getEditableText().toString();
                if (inputCommunity.isEmpty() || inputCommunity.equals("null")) {
                    inputCommunity = DEFAULT_SNMP_COMUNITY;
                }
                data.putExtra(COMMUNITY_KEY, inputCommunity);
            } else {
                // v3
                data.putExtra(USER_KEY, userField.getEditableText().toString());
                data.putExtra(USER_PASSPHRASE_KEY, passwordField.getEditableText().toString());
                data.putExtra(ENC_KEY, encryptField.getEditableText().toString());
            }

            Log.d(TAG, data.toString());
            setResult(RESULT_OK, data);
            CockpitStateManager.getInstance().getIsInSessionTimeoutObservable().setValueAndTriggerObservers(false);
            CockpitStateManager.getInstance().getIsInTimeoutsObservable().setValueAndTriggerObservers(false);
            finish();
        }
    }

    /**
     * Set up the {@link androidx.appcompat.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startProtection(this);
    }

    //Method to show for each SNMP version the correct input fields.
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //create spinner and add a notification box, when selected a item.
        String text = parent.getItemAtPosition(position).toString();
        Log.i(TAG, "selected version " + text);

        //Using the each row of the table, to hide or show them, depending on the SNMP Version.
        TableRow community = findViewById(R.id.tablerow_community);
        TableRow username = findViewById(R.id.tablerow_username);
        TableRow password = findViewById(R.id.tablerow_password);
        TableRow encrypt = findViewById(R.id.tablerow_encrypt);

        snmpSpinner = findViewById(R.id.snmp_spinner);
        Log.i(TAG, snmpSpinner.getSelectedItem().toString());

        switch (position) {
            case 0: //SNMP Version 1 or 2c
                community.setVisibility(View.VISIBLE);
                username.setVisibility(View.INVISIBLE);
                password.setVisibility(View.INVISIBLE);
                encrypt.setVisibility(View.INVISIBLE);
                break;
            case 1: //SNMP Version 3
                community.setVisibility(View.INVISIBLE);
                username.setVisibility(View.VISIBLE);
                password.setVisibility(View.VISIBLE);
                encrypt.setVisibility(View.VISIBLE);
                break;
            default:
                throw new IllegalArgumentException("not supported position value");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restartTrigger(this);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // just do nothing
    }

    /**
     * QR-scanner-method show flashlight - if preferences say to do so
     */
    public void onScan() {
        CockpitPreferenceManager cockpit = SnmpCockpitApp.getPreferenceManager();
        QrScannerActivityHelper scannerActivityHelper = new QrScannerActivityHelper(this);
        if (cockpit.showFlashlightHint()) {
            new AlertHelper(this).showFlashlightHintDialog(scannerActivityHelper, false);
        } else {
            scannerActivityHelper.startDeviceScanner();
        }
    }

    /**
     * handle and process device qr code scan result
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, R.string.connection_attempt_canceled, Toast.LENGTH_LONG).show();
            } else {
                qrString = result.getContents();
                Log.d(TAG, qrString);

                DeviceQrCode deviceQrCode = getDeviceQrCode(qrString);
                if (deviceQrCode != null) {
                    SnmpEndpoint endpoint = deviceQrCode.getEndpoint();
                    if (endpoint != null) {
                        Log.d(TAG, "scanned device qr code: " + deviceQrCode);
                        clearValidatedFields();
                        autoFillEditTexts(deviceQrCode, endpoint);
                    } else {
                        Toast.makeText(this, getString(R.string.invalid_snmp_endpoint_information_in_qr_code_toast), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.toast_no_valid_device_qr_code, Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Get the QR-Code
     */
    private DeviceQrCode getDeviceQrCode(String qrString) {
        try {
            return new ObjectMapper().readValue(qrString, DeviceQrCode.class);
        } catch (IOException e) {
            Log.e(TAG, "error reading qr code " + e.getMessage());
        }
        return null;
    }

    /**
     * Autofill method, which gets the qr-code and filling with JACKSON each text gap.
     */
    private void autoFillEditTexts(DeviceQrCode deviceQrCode, @NonNull SnmpEndpoint endpoint) {
        String qrCode = qrString;
        Log.d(TAG, "found qr code: " + qrCode);

        // set proper ipv6 switch state
        boolean isIpv6SwitchEnabled = false;
        if (endpoint.isIpv6()) {
            isIpv6SwitchEnabled = true;
        }
        ipv6EnabledSwitch.setChecked(isIpv6SwitchEnabled);

        if (deviceQrCode.getPw().isEmpty() && deviceQrCode.getEnc().isEmpty()) {
            // if there is no password and encoder --> take SNMP V1/V2c.
            snmpSpinner.setSelection(0);
            portField.setText(String.valueOf(endpoint.getPort()));

            communityField.setText(deviceQrCode.getUser());
            hostField.setText(endpoint.getIpAddress());
        } else {
            // SNMP V3
            snmpSpinner.setSelection(1);
            hostField.setText(endpoint.getIpAddress());
            portField.setText(String.valueOf(endpoint.getPort()));
            userField.setText(deviceQrCode.getUser());
            passwordField.setText(deviceQrCode.getPw());
            encryptField.setText(deviceQrCode.getEnc());
        }
    }

    /*
     * showError method, which gives the edittext field a warning hint, if the parameter is wrong.
     */
    private void showError(EditText text) {
        //Animation if pattern isn´t matching
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        text.startAnimation(shake);
        //Text output
        text.setError(getString(R.string.invalid_user_input));
    }

    private static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    private static boolean isIPv6Address(final String input) {
        return IPV6_PATTERN.matcher(input).matches();
    }

    public static boolean isPortValid(final String input) {
        return PORT_PATTERN.matcher(input).matches();
    }

    /**
     * Method which checks the pattern of every input.
     */
    private boolean isInputValid() {
        String inputIpString = hostField.getText().toString();
        boolean isIpv6 = ipv6EnabledSwitch.isChecked();

        //TODO: Issue: If you have a String with the wrong pattern and you scan the next barcode, the error massage, is not disappearing.

        if (isIpv6) {
            // check for correct ipv6 address
            if (!isIPv6Address(inputIpString)) {
                showError(hostField);
                return false;
            } else {
                // ipv6 address is valid!
                hostField.setError(null);
            }
        } else {
            // ipv4
            // check for correct ipv4 address
            if (!isIPv4Address(inputIpString)) {
                showError(hostField);
                return false;
            } else {
                // ip v4 address is valid
                hostField.setError(null);
            }
        }

        String inputPort = portField.getText().toString();
        // allow empty port field, we use default fallback
        if (!inputPort.isEmpty() && !isPortValid(inputPort)) {
            showError(portField);
            return false;
        }
        return true;
    }

    /**
     * method to clear all validated fields (after a new qr device code was scanned with success)
     */
    private void clearValidatedFields() {
        hostField.setError(null);
        portField.setError(null);
    }

    @Override
    public void restartQueryCall() {
        // do nothing
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connect_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_connect) {
            item.setEnabled(false);
            // start connection
            this.onConnect();
            item.setEnabled(true);
            return true;
        } else if (item.getItemId() == R.id.action_scan_qr_code) {
            // start qr code scanning
            item.setEnabled(false);
            this.onScan();
            item.setEnabled(true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}

