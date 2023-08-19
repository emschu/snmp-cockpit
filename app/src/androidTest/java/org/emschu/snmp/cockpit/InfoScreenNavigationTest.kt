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
import androidx.navigation.compose.rememberNavController
import org.emschu.snmp.cockpit.ui.screens.InfoScreen
import org.emschu.snmp.cockpit.ui.screens.infoScreens
import org.junit.Rule
import org.junit.Test

class InfoScreenNavigationTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testInfoScreen() {
        composeTestRule.setContent {
            InfoScreen(navController = rememberNavController(), mainViewModel = mainViewModel)
        }
        composeTestRule.onNodeWithContentDescription("Logo")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("app info text")
            .assertExists()
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("license info text")
            .assertExists()
            .assertIsDisplayed()

        val infoScreenTitles = infoScreens.map { appContext.getString(it.titleResId) }
            .toList()
        assertNodesWithTextExistAndAreDisplayed(infoScreenTitles)
        // TODO test actions after clicking on menu items
    }

    fun assertNodesWithTextExistAndAreDisplayed(nodes: List<String>) {
        nodes.forEach {
            composeTestRule.onNodeWithText(it)
                .assertExists()
                .assertIsDisplayed()
        }
    }
}