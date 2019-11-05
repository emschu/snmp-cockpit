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


import android.content.Intent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import android.widget.ProgressBar;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CockpitMainActivityTest {

    @Rule
    public ActivityTestRule<CockpitMainActivity> rule = new ActivityTestRule<>(CockpitMainActivity.class, true, true);

    @Before
    public void setUp() throws Exception {
        rule.launchActivity(new Intent());
    }

    @Test
    public void testStartup() {
        CockpitMainActivity activity = rule.getActivity();
        assertEquals(activity.getResources().getString(R.string.app_name), activity.getTitle());
        ConstraintLayout cl = activity.findViewById(R.id.fragment_container);
        // test our fragment container is not empty
        assertTrue(cl.getChildCount() > 0);

        ProgressBar pb = activity.findViewById(R.id.app_progress_bar);
        assertNotNull(pb);
        // assert support toolbar is available
        assertNull(activity.getActionBar());
        assertNotNull(activity.findViewById(R.id.toolbar));
        assertNotNull(activity.getSupportActionBar());
    }
}
