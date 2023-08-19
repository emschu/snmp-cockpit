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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.emschu.snmp.cockpit.ui.components.ToggleChip
import org.junit.Rule
import org.junit.Test

class ToggleChipComponentTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTextDisplayWorksForBothStates() {
        val testActivatedText = "activated"
        val testDisabledText = "disabled"
        val toggleState = mutableStateOf(true)

        composeTestRule.setContent {
            ToggleChip(
                onClick = { toggleState.value = !toggleState.value },
                onText = testActivatedText,
                toggleState = toggleState.value,
                offText = testDisabledText
            )
        }
        composeTestRule.onNodeWithContentDescription("Pin")
            .assertIsDisplayed()
            .assert(hasText(testActivatedText))
        toggleState.value = !toggleState.value
        composeTestRule.onNodeWithContentDescription("Pin")
            .assertIsDisplayed()
            .assert(hasText(testDisabledText))
    }
}