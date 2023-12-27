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

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.emschu.snmp.cockpit.snmp.OIDCatalog
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OidCatalogTest : AbstractCockpitAppTest() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testIfOidCatalogWorks() {
        Assert.assertEquals("system", OIDCatalog.getAsnByOid("1.3.6.1.2.1.1"))
        Assert.assertEquals("laEntry", OIDCatalog.getAsnByOid("1.3.6.1.4.1.2021.10.1"))
        Assert.assertEquals("laIndex", OIDCatalog.getAsnByOid("1.3.6.1.4.1.2021.10.1.1.1"))

        Assert.assertEquals("", OIDCatalog.getAsnByOid(""))
        // these oid do not exist
        Assert.assertEquals("", OIDCatalog.getAsnByOid("2.1"))
        Assert.assertEquals("ipAdEntAddr", OIDCatalog.getAsnByOid("1.3.6.1.2.1.4.20.1.1.192.168.178.21"))
    }
}