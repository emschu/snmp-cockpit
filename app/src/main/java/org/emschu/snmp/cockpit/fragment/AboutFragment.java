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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.emschu.snmp.cockpit.BuildConfig;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.AboutHelperActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * main fragment for about section
 */
public class AboutFragment extends Fragment {


    public static final String TAG = AboutFragment.class.getName();
    public static final String MESSAGE_KEY = "Message";
    public static final String FILE_KEY = "File";

    public AboutFragment() {
        // android needs an empty fragment constructor
    }

    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        String[] arrays = getResources().getStringArray(R.array.about_screen);
        ListView listView = view.findViewById(R.id.lv_about_items);

        if (getActivity() == null) {
            return null;
        }

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, arrays);
        listView.setAdapter(listViewAdapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Intent intent = new Intent(view1.getContext(), AboutHelperActivity.class);
            switch (position) {
                case 0: // License
                    intent.putExtra(FILE_KEY, "file:///android_asset/license.html");
                    intent.putExtra(MESSAGE_KEY, "license");
                    break;
                case 1: // WIFI QR code Schema
                    intent.putExtra(FILE_KEY, "file:///android_asset/wifi_code_doc.html");
                    intent.putExtra(MESSAGE_KEY, "wifiQR");
                    break;
                case 2: // Device QR-Code Schema
                    intent.putExtra(FILE_KEY, "file:///android_asset/device_code_doc.html");
                    intent.putExtra(MESSAGE_KEY, "deviceQR");
                    break;
                case 3:
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://github.com/emschu/snmp-cockpit"));
                    startActivity(i);
                    break;
                case 4: // Used Libraries
                    new LibsBuilder()
                            .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                            .withActivityTitle(getString(R.string.used_libraries))
                            .withAboutAppName(getString(R.string.app_name))
                            .withAboutIconShown(true)
                            .withAutoDetect(true)
                            .withAboutDescription("2018-2019 Marius Schuppert, Ömer Ergün, Steven Pham<br/><br/>"
                                    + getString(R.string.license_about_text)
                                    + "<br/>"
                                    + String.format(getString(R.string.app_build_time), (new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale.ENGLISH))
                                    .format(new Date(BuildConfig.BUILD_TIMESTAMP))))
                            .start(getActivity());
                    break;
                case 5:
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto","emschu@mailbox.org", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback SNMP Cockpit Android APP");
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email_title)));
                    break;
                case 6:
                    intent.putExtra(FILE_KEY, "file:///android_asset/privacy_policy.html");
                    intent.putExtra(MESSAGE_KEY, "deviceQR");
                default:
                    Log.w(TAG, "unexpected index: " + position);
            }
            if (intent.hasExtra(FILE_KEY)) {
                startActivity(intent);
            }
        });
        return view;
    }
}
