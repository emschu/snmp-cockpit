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

import android.app.Instrumentation
import android.os.Binder
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.BeforeClass

abstract class AbstractPermissionResetTest : AbstractCockpitAppTest() {

    protected fun grantRuntimePermissions(instrumentation: Instrumentation, permissions: List<String>) {
        val appContext = instrumentation.targetContext
        permissions.forEach {
            if (it == "android.permission.INTERNET") {
                // this is not changeable by android and seems to be granted always
                return
            }
            instrumentation.uiAutomation.grantRuntimePermissionAsUser(
                appContext.packageName, it, Binder.getCallingUserHandle()
            )
        }
    }

    /**
     * clicks on "allow" in permission dialog
     */
    protected fun grantPermissionDialogs() {
        device().wait(
            Until.findObject(
                By.res("com.android.permissioncontroller:id/permission_allow_one_time_button")
            ), 5000
        )?.click()
    }

    /**
     * clicks on "deny" in permission dialog
     */
    protected fun denyPermissionDialogs() {
        device().wait(
            Until.findObject(
                By.res("com.android.permissioncontroller:id/permission_deny_button")
            ), 5000
        )?.click()
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            setupSnmpCockpitApp()
        }
    }
}