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

import org.emschu.snmp.cockpit.ui.components.APP_FRAGMENTS
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.components.screenToFragment
import org.emschu.snmp.cockpit.ui.components.singlePageFragments
import org.junit.Assert
import org.junit.Test

class NavFragmentsTest {

    @Test
    fun testSinglePageFragments() {
        singlePageFragments.forEach {
            Assert.assertTrue(
                Screen.values()
                    .contains(it)
            )
            val frag = screenToFragment(it)

            Assert.assertNotNull(frag)
            // single page fragments do not have an entry in APP_FRAGMENTS
            Assert.assertFalse(APP_FRAGMENTS.contains(frag))
        }
    }
}