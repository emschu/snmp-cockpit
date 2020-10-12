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

package org.emschu.snmp.cockpit.fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.BufferedInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import androidx.appcompat.app.AppCompatActivity;
import org.emschu.snmp.cockpit.BuildConfig;
import org.emschu.snmp.cockpit.R;

/**
 * about helper fragment which wraps functionality in "about" section
 *
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AboutHelperFragment extends AppCompatActivity {

    public static final String TAG = AboutHelperFragment.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_about_helper);

        WebView webView = this.findViewById(R.id.wv_aboutScreen);
        webView.getSettings().getJavaScriptEnabled();
        webView.setNetworkAvailable(false);
        webView.setWebChromeClient(new WebChromeClient());

        String myURL = getIntent().getStringExtra("File");
        String selectedItem = getIntent().getStringExtra("Message");

        if (selectedItem != null && selectedItem.equals("version")) {
            String html = htmlFileToString();
            if (html != null) {
                html = html.replace("&lt;VERSION&gt;", getVersionNumber());
                html = html.replace("&lt;BUILD&gt;", getBuildTimestamp());
                webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
                Log.d(TAG, html);
            }
        } else {
            webView.loadUrl(myURL);
        }
    }

    private String htmlFileToString() {
        String result = null;
        try (Scanner s = new Scanner(new BufferedInputStream(getAssets().open("assets.html"))).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : "";
        } catch (Exception e) {
            Log.w(TAG, "error fetching basic html: " + e.getMessage());
        }
        return result;
    }

    private String getVersionNumber() {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "name not found exception: " + e.getMessage());
        }
        if (info == null) {
            // fallback
            return "0";
        }
        return info.versionName;
    }

    private String getBuildTimestamp() {
        return new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale.ENGLISH)
                .format(new Date(BuildConfig.BUILD_TIMESTAMP));
    }
}
