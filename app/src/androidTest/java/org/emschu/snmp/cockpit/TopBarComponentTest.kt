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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.components.screenToFragment
import org.junit.Rule
import org.junit.Test

class TopBarComponentTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHomeScreenTopBar() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.HOME)
        }
        val fragment = screenToFragment(Screen.HOME)

        composeTestRule.onNodeWithContentDescription("menu")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("refresh")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(appContext.getString(fragment.title))
            .assertExists()
            .assertIsDisplayed()

        SnmpCockpitApp.deviceManager.add(
            DeviceConfiguration(
                DeviceConfiguration.SNMP_VERSION.v1,
                targetIp = "127.0.0.1"
            ), appContext
        )
        composeTestRule.onNodeWithContentDescription("refresh")
            .assertDoesNotExist()
    }

    @Test
    fun testMibCatalogTopBar() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.MIBCATALOG)
        }
        val fragment = screenToFragment(Screen.MIBCATALOG)

        composeTestRule.onNodeWithContentDescription("menu")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("add mib catalog to app")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(fragment.title))
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testLoginTopBar() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.LOGIN)
        }
        val fragment = screenToFragment(Screen.LOGIN)

        composeTestRule.onNodeWithContentDescription("menu")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("login")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("qrcodescanner")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(fragment.title))
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testNetworkTopBar() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.NETWORK)
        }
        val fragment = screenToFragment(Screen.NETWORK)

        composeTestRule.onNodeWithContentDescription("menu")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("refresh network button")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(fragment.title))
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testSettingsTopBar() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.SETTINGS)
        }
        val fragment = screenToFragment(Screen.SETTINGS)

        composeTestRule.onNodeWithContentDescription("menu")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(fragment.title))
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testDeviceDetailsTopBar() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.DEVICE_DETAIL)
        }
        val fragment = screenToFragment(Screen.DEVICE_DETAIL)

        composeTestRule.onNodeWithContentDescription("menu")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("refresh")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("more options")
            .assertExists()
            .assertIsDisplayed()

        // the dropdown items are not yet visible
        composeTestRule.onNodeWithContentDescription("remove device")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("device qr code")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("connection information")
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(appContext.getString(fragment.title))
            .assertExists()
            .assertIsDisplayed()

        // test dynamic title of single page fragments - if set
        val testScreenTitle = "TestScreenTitle #234"
        mainViewModel.screenTitle.value = testScreenTitle
        composeTestRule.onNodeWithText(testScreenTitle)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("refresh")
            .assertExists()
            .performClick()
        // nothing should happen related to dropdown item's visibility state
        composeTestRule.onNodeWithContentDescription("remove device")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("device qr code")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("connection information")
            .assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("more options")
            .assertExists()
            .performClick()

        // now the dropdown action menu items are visible
        composeTestRule.onNodeWithContentDescription("remove device")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("device qr code")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("connection information")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testInfoScreenSection() {
        composeTestRule.setContent {
            ShowTopBar(currentScreen = Screen.INFO_SECTION)
        }
        composeTestRule.onNodeWithContentDescription("menu")
            .assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("arrow back")
            .assertExists()
            .assertIsDisplayed()
    }
}