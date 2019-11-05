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

import com.google.zxing.integration.android.IntentIntegrator;

import org.emschu.snmp.cockpit.R;

/**
 * this class wrayps functionality for wifi qr code scanner
 */
public class QrScannerActivityHelper {

    private Activity activity;

    public QrScannerActivityHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * starting the zxing intent integrator.
     */
    public void startWifiScanner() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(activity);
        intentIntegrator.setPrompt(activity.getString(R.string.scan_wifi_code_label));
        intentIntegrator.setBarcodeImageEnabled(true);
        intentIntegrator.setOrientationLocked(false);
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        intentIntegrator.initiateScan();
    }

    /**
     * start device qr code scanner
     */
    public void startDeviceScanner() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(activity);
        intentIntegrator.setPrompt(activity.getString(R.string.scan_device_code_label));
        intentIntegrator.setBarcodeImageEnabled(true);
        intentIntegrator.setOrientationLocked(false);
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        intentIntegrator.initiateScan();
    }
}
