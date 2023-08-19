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

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.emschu.snmp.cockpit.ui.screens.NetworkDetailsView
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NetworkDetailsTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun content() {
        composeTestRule.activity.setContent {
            NetworkDetailsView(viewModel = mainViewModel)
        }
        acceptPermissionDialog()
    }

    @Test
    fun shouldDisplaySomeInformation() {
        // ... but wifi
        assertNetworkDetailsScreenLabels()

        composeTestRule.onNodeWithText("00:13:10:85:fe:01")
            .assertExists()
        composeTestRule.onNodeWithText("AndroidWifi")
            .assertExists()
    }

    private fun assertNetworkDetailsScreenLabels() {
        composeTestRule.onNodeWithText("SSID")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("BSSID")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("IP")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("DNS")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Gateway")
            .assertIsDisplayed()
    }
}