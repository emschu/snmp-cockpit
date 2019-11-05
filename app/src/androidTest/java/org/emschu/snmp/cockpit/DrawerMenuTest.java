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
import android.view.Gravity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.DrawerMatchers;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class DrawerMenuTest {

    @Rule
    public ActivityTestRule<CockpitMainActivity> rule = new ActivityTestRule<>(CockpitMainActivity.class, true, true);

    @Before
    public void setUp() throws Exception {
        CockpitStateManager.getInstance().setInTestMode(true);

        rule.launchActivity(new Intent());
    }

    @Test
    public void openAllDrawerMenus() {
        int[] menuIds = new int[]{
                R.id.nav_main_monitoring_view,
                R.id.nav_mib_catalog,
                R.id.nav_manage_own_queries,
                R.id.nav_settings,
                R.id.nav_about

        };
        int[] appTitles = new int[] {
                R.string.app_name,
                R.string.menu_catalog_label,
                R.string.title_activity_own_queries,
                R.string.title_activity_settings,
                R.string.title_activity_about
        };

        int i = 0;
        for (int menuItemId : menuIds) {
            Espresso.onView(withId(R.id.drawer_layout))
                    .check(ViewAssertions.matches(DrawerMatchers.isClosed(Gravity.LEFT)))
                    .perform(DrawerActions.open());
            Espresso.onView(withId(R.id.nav_view))
                    .perform(NavigationViewActions.navigateTo(menuItemId));
            Assert.assertEquals(rule.getActivity().getString(appTitles[i]), rule.getActivity().getTitle());
            Espresso.onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());
            i++;
        }
    }
}
