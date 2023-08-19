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

package org.emschu.snmp.cockpit.db

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.emschu.snmp.cockpit.AbstractCockpitAppTest
import org.emschu.snmp.cockpit.MainActivity
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class CockpitDbHelperTest : AbstractCockpitAppTest() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBasicInitializationRetrievalAndDeletion() {
        Assert.assertNotNull(appContext)
        val cockpitDbHelper = CockpitDbHelper(appContext)
        Assert.assertNotNull(cockpitDbHelper.readableDatabase)
        Assert.assertNotNull(cockpitDbHelper.writableDatabase)
        // ensure default data is present
        if (cockpitDbHelper.queryRowCount == 0) {
            cockpitDbHelper.createDefaultEntries(cockpitDbHelper.writableDatabase)
        }
        val customQueries = cockpitDbHelper.getCustomQueries()
        Assert.assertNotNull(customQueries)

        Assert.assertTrue(customQueries.isNotEmpty())

        Assert.assertEquals(customQueries.size, cockpitDbHelper.queryRowCount)

        customQueries.forEach {
            cockpitDbHelper.removeQuery(it.id)
        }
        Assert.assertEquals(0, cockpitDbHelper.queryRowCount)
        val shouldBeEmptyList = cockpitDbHelper.getCustomQueries()
        Assert.assertTrue(shouldBeEmptyList.isEmpty())
        cockpitDbHelper.close()
    }

    @Test
    fun testEntryRetrieval() {
        val cockpitDbHelper = CockpitDbHelper(appContext)
        // deleting non-existent entries should work
        Assert.assertNull(cockpitDbHelper.getCustomQueryById(-345))
        Assert.assertNull(cockpitDbHelper.getCustomQueryById(345))
        Assert.assertNull(cockpitDbHelper.getCustomQueryById(0))

        val newId = cockpitDbHelper.addNewQuery("1.2.3", "unitTestEntry", true)
        Assert.assertNotNull(cockpitDbHelper.getCustomQueryById(newId))

        var isFound = false
        cockpitDbHelper.getCustomQueries()
            .forEach {
                if (it.id == newId) {
                    isFound = true
                }
            }
        Assert.assertTrue(isFound)
    }

    @Test
    fun testEntryDeletion() {
        val cockpitDbHelper = CockpitDbHelper(appContext)
        // deleting non-existent entries should work
        Assert.assertNull(cockpitDbHelper.getCustomQueryById(-345))
        cockpitDbHelper.removeQuery(-345)
        Assert.assertNull(cockpitDbHelper.getCustomQueryById(-345))

        val newId = cockpitDbHelper.addNewQuery("1.2.3", "unitTestEntry", true)
        Assert.assertNotNull(cockpitDbHelper.getCustomQueryById(newId))
        cockpitDbHelper.removeQuery(newId)
        Assert.assertNull(cockpitDbHelper.getCustomQueryById(newId))
    }
}