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

import androidx.appcompat.app.AppCompatActivity;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.R;

/**
 * this class is a wrapper around the settings fragment defined here:
 * {@link CockpitPreferenceManager.GeneralPreferenceFragment}
 *
 * this activity is displayed when app has no secure network
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class BlockedSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_settings);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
