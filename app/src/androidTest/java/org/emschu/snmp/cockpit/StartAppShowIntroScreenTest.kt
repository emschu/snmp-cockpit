/*
 * snmp-cockpit
 *
 * Copyright (C) 2018-2023
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit

import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class StartAppShowIntroScreenTest : AbstractCockpitAppTest() {
    @Before
    fun setUp() {
        acceptPermissionDialog()
    }

    @Test
    fun testIntroductionScreenIsShown() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val welcomeText = device().wait(Until.findObject(By.text("Welcome...")), 5000)
        Assert.assertNotNull(welcomeText)
        val nextBtn = device().wait(Until.findObject(By.desc("NEXT")), 5000)
        Assert.assertNotNull(nextBtn)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeAppStartup() {
            Assert.assertNotNull(SnmpCockpitApp)
            val edit = SnmpCockpitApp.preferenceManager().sharedPreferences.edit()
            edit.putBoolean(CockpitPreferenceManager.KEY_SHOW_WELCOME_DIALOG, false)
            edit.apply()
            Assert.assertFalse(SnmpCockpitApp.preferenceManager().isWelcomeScreenShown)
        }
    }
}