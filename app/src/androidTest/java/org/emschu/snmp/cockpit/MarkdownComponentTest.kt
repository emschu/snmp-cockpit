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

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.emschu.snmp.cockpit.ui.components.MarkdownFromAsset
import org.emschu.snmp.cockpit.ui.sources.MarkdownAssetFile
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class MarkdownComponentTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun testMarkdownComponentForLicenseFile() {
        // if the enum items count changes, adjust this test
        Assert.assertEquals(4, MarkdownAssetFile.values().size)

        composeTestRule.setContent {
            MarkdownFromAsset(assetFile = MarkdownAssetFile.LICENSE)
        }
        Espresso.onView(isRoot())
            .check(ViewAssertions.matches(isDisplayed()))
    }

    @Test
    fun testMarkdownComponentForPrivPolicyFile() {
        composeTestRule.setContent {
            MarkdownFromAsset(assetFile = MarkdownAssetFile.PRIVACY_POLICY)
        }
        Espresso.onView(isRoot())
            .check(ViewAssertions.matches(isDisplayed()))
    }

    @Test
    fun testMarkdownComponentForQrCodeGuide() {
        composeTestRule.setContent {
            MarkdownFromAsset(assetFile = MarkdownAssetFile.DOCS_DEVICE_QR_CODE)
        }
        Espresso.onView(isRoot())
            .check(ViewAssertions.matches(isDisplayed()))
    }

    @Test
    fun testMarkdownComponentForMibCatalogGuide() {
        composeTestRule.setContent {
            MarkdownFromAsset(assetFile = MarkdownAssetFile.DOCS_MIB_CATALOG)
        }
        Espresso.onView(isRoot())
            .check(ViewAssertions.matches(isDisplayed()))
    }
}