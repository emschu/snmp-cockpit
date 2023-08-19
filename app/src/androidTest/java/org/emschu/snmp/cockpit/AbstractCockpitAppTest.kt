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
import android.content.Context
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import org.emschu.android.treeview.TreeViewModel
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.components.TopAppBarLayout
import org.emschu.snmp.cockpit.ui.viewmodel.*
import org.junit.BeforeClass
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class AbstractCockpitAppTest {
    val mainViewModel = MainViewModel()
    val loginViewModel = LoginViewModel(SavedStateHandle())
    val treeViewModel = TreeViewModel(SavedStateHandle())

    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val appContext: Context = instrumentation.targetContext

    fun device(): UiDevice {
        return UiDevice.getInstance(instrumentation)
    }

    @Composable
    fun ShowTopBar(currentScreen: Screen = Screen.HOME, isSpinnerActivated: Boolean = false) {
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        val navController = rememberNavController()
        val deviceDetailViewModel = viewModel<DeviceDetailViewModel>()
        val queryDetailViewModel = viewModel<QueryDetailViewModel>()
        val queryViewModel = viewModel<QueryViewModel>()

        TopAppBarLayout(
            mainViewModel = mainViewModel,
            deviceDetailViewModel = deviceDetailViewModel,
            queryDetailViewModel = queryDetailViewModel,
            loginViewModel = loginViewModel,
            queryViewModel = queryViewModel,
            scope = scope,
            scaffoldState = scaffoldState,
            isSpinnerActivated = isSpinnerActivated,
            currentScreen = currentScreen,
            navController = navController
        )
    }

    internal fun acceptPermissionDialog() {
        try {
            val findObject = UiDevice.getInstance(instrumentation)
                .findObject(
                    UiSelector()
                        .text("While using the app")
                )
            findObject.click()
        } catch (ex: UiObjectNotFoundException) {
            return
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupSnmpCockpitApp() {
            SnmpCockpitApp.preferenceManager().setWelcomeScreenShown()
        }
    }
}