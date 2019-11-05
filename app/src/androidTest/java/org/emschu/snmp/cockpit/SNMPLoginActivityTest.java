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


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.emschu.snmp.cockpit.activity.SNMPLoginActivity;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class SNMPLoginActivityTest {

    @Rule
    public ActivityTestRule<SNMPLoginActivity> rule = new ActivityTestRule<>(SNMPLoginActivity.class);

    @Test
    public void testStartup() {
        SNMPLoginActivity activity = rule.getActivity();
        assertEquals(activity.getString(R.string.title_activity_snmplogin), activity.getTitle());
        // TODO
    }
}
