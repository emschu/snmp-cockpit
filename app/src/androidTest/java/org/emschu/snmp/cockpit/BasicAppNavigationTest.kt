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

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import org.emschu.snmp.cockpit.ui.components.NavigationFragment
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.viewmodel.DeviceDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryViewModel
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BasicAppNavigationTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
    @Before
    fun setup() {
        composeTestRule.setContent {
            val deviceDetailViewModel = viewModel<DeviceDetailViewModel>()
            val queryDetailViewModel = viewModel<QueryDetailViewModel>()
            val queryViewModel = viewModel<QueryViewModel>()
            CockpitTheme {
                MainView(
                    mainViewModel, deviceDetailViewModel, queryDetailViewModel, loginViewModel, treeViewModel,
                    queryViewModel
                )
            }
        }
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        Assert.assertNotNull(appContext)
        Assert.assertEquals("org.emschu.snmp.cockpit.debug", appContext.packageName)
    }

    @Test
    fun StartAppAndOpenLoginScreen() {
        composeTestRule.onNodeWithContentDescription("login_fab")
            .performClick()

        composeTestRule.onAllNodesWithText(appContext.getString(NavigationFragment.Login.title))
            .assertCountEquals(1)

        composeTestRule.onNodeWithText(appContext.getString(R.string.user_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.password_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.snmpv3_key))
            .assertIsDisplayed()
    }

    @Test
    fun StartAppAndOpenMibCatalog() {
        openScreen(composeTestRule, appContext.getString(NavigationFragment.MIBCatalog.title))

        composeTestRule.onAllNodesWithText(appContext.getString(NavigationFragment.MIBCatalog.title))
            .assertCountEquals(2)
    }

    @Test
    fun StartAppAndOpenNetworkDetails() {
        openScreen(composeTestRule, appContext.getString(NavigationFragment.Network.title))

        composeTestRule.onAllNodesWithText(appContext.getString(NavigationFragment.Network.title))
            .assertCountEquals(2)
    }

    @Test
    fun StartAppAndOpenQueries() {
        openScreen(composeTestRule, appContext.getString(NavigationFragment.Queries.title))

        composeTestRule.onAllNodesWithText(appContext.getString(NavigationFragment.Queries.title))
            .assertCountEquals(2)
    }

    @Test
    fun StartAppAndOpenInfo() {
        openScreen(composeTestRule, appContext.getString(NavigationFragment.Info.title))

        composeTestRule.onAllNodesWithText(appContext.getString(NavigationFragment.Info.title))
            .assertCountEquals(2)
    }
    // TODO test settings screen
}

private fun openScreen(composeTestRule: ComposeContentTestRule, title: String) {
    composeTestRule.onNodeWithContentDescription("menu")
        .assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("menu")
        .assertExists()
        .performClick()
    composeTestRule.onNodeWithText(title)
        .assertExists()
        .performClick()
    composeTestRule.onNodeWithContentDescription("menu")
        .assertIsDisplayed()
}