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

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.emschu.snmp.cockpit.model.CustomQuery
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.screens.CustomQueryView
import org.emschu.snmp.cockpit.ui.sources.CustomQuerySource
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.viewmodel.QueryViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CustomQueryTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    lateinit var queryViewModel: QueryViewModel

    @OptIn(ExperimentalMaterialApi::class)
    @Before
    fun setup() {
        // Start the app
        composeTestRule.setContent {
            queryViewModel = QueryViewModel()
            CockpitTheme {
                ShowTopBar(currentScreen = Screen.QUERIES)
                CustomQueryView(
                    mainViewModel, queryViewModel,
                    rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
                )
            }
        }
    }

    @Test
    fun testSingleQueryIsListed() {
        val testQuery = CustomQuery(
            id = 1,
            oid = "1.1.1",
            name = "TestQuery",
            isSingleQuery = false,
            isShowInDetailsTab = false,
        )
        composeTestRule.waitForIdle()
        CustomQuerySource.allCustomQueries.postValue(listOf(testQuery))
        composeTestRule.onNodeWithText(testQuery.name).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("add query").assertExists().assertIsDisplayed()
    }

    @Test
    fun testQueriesAreListed() {
        val testQuery = CustomQuery(
            id = 1,
            oid = "1.1.1",
            name = "TestQuery",
            isSingleQuery = false,
            isShowInDetailsTab = false,
        )
        val testQuery2 = testQuery.copy(
            id = 2,
            name = "TestQuery2",
        )
        val testQuery3 = testQuery.copy(
            id = 3,
            name = "TestQuery3"
        )
        CustomQuerySource.allCustomQueries.postValue(listOf(testQuery, testQuery2, testQuery3))

        composeTestRule.onNodeWithContentDescription("add query").assertExists().assertIsDisplayed()

        composeTestRule.onNodeWithText(testQuery.name).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithText(testQuery2.name).assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithText(testQuery3.name).assertExists().assertIsDisplayed()
    }
}