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
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import org.emschu.snmp.cockpit.R;

/**
 * about helper fragment which wraps functionality in "about" section
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AboutHelperActivity extends AppCompatActivity {

    public static final String TAG = AboutHelperActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_about_helper);

        WebView webView = this.findViewById(R.id.wv_aboutScreen);
        webView.getSettings().getJavaScriptEnabled();

        String myURL = getIntent().getStringExtra("File");
        webView.loadUrl(myURL);
    }
}
