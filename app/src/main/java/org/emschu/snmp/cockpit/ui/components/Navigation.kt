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

package org.emschu.snmp.cockpit.ui.components


import androidx.annotation.StringRes
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Aod
import androidx.compose.material.icons.twotone.Dvr
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.PhonelinkErase
import androidx.compose.material.icons.twotone.QueryBuilder
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.SettingsEthernet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.emschu.snmp.cockpit.R

/**
 * This class encapsulates a mutable shared flow to be consumed as provider of the current screen the user should see
 */
class AppNavigator {
    private val _sharedFlow = MutableSharedFlow<Screen>(extraBufferCapacity = 1)
    val sharedFlow = _sharedFlow.asSharedFlow()
    var lastScreen: Screen? = null
        private set
    private var lastLastScreen: Screen? = null

    init {
        // initial value
        this._sharedFlow.tryEmit(Screen.HOME)
        this.sharedFlow.onEach {
            this.lastScreen = this.lastLastScreen
            this.lastLastScreen = it
        }.launchIn(CoroutineScope(Job() + Dispatchers.Default))
    }

    fun navigateTo(navTarget: Screen) {
        _sharedFlow.tryEmit(navTarget)
    }
}

@Stable
sealed class NavigationFragment(
    @StringRes val title: Int,
    val id: Screen,
    val icon: @Composable () -> (Unit) = {},
    val isAction: Boolean = false,
) {

    object Home : NavigationFragment(R.string.app_name, Screen.HOME, {
        Icon(Icons.TwoTone.Aod, "home")
    })

    object MIBCatalog : NavigationFragment(R.string.menu_catalog_label, Screen.MIBCATALOG, {
        Icon(Icons.TwoTone.Dvr, "start")
    })

    object Settings : NavigationFragment(R.string.menu_settings_label, Screen.SETTINGS, {
        Icon(Icons.TwoTone.Settings, "settings")
    })

    object Queries : NavigationFragment(R.string.menu_own_queries_label, Screen.QUERIES, {
        Icon(Icons.TwoTone.QueryBuilder, "custom queries")
    })

    object Network : NavigationFragment(R.string.menu_network_details_label, Screen.NETWORK, {
        Icon(Icons.TwoTone.SettingsEthernet, "network details")
    })

    object Info : NavigationFragment(R.string.menu_about_label, Screen.INFO, {
        Icon(Icons.TwoTone.Info, "info")
    })

    object InfoSection : NavigationFragment(R.string.menu_about_label, Screen.INFO_SECTION)

    object RemoveDevicesAction :
        NavigationFragment(R.string.menu_action_device_eject_label, Screen.REMOVE_DEVICES_ACTION, {
            Icon(Icons.TwoTone.PhonelinkErase, "remove all devices")
        }, true)

    // invisible menu items
    object Login : NavigationFragment(R.string.title_activity_snmplogin, Screen.LOGIN)
    object DeviceDetail : NavigationFragment(R.string.title_activity_tabbed_device, Screen.DEVICE_DETAIL)
    object QueryDetail :
        NavigationFragment(R.string.title_activity_single_query_result, Screen.QUERY_DETAIL)
}

val singlePageFragments = listOf(
    Screen.QUERY_DETAIL,
    Screen.DEVICE_DETAIL,
    Screen.INFO_SECTION,
)

enum class Screen {
    HOME, MIBCATALOG, SETTINGS, LOGIN, QUERIES, NETWORK, INFO, INFO_SECTION, DEVICE_DETAIL, QUERY_DETAIL, REMOVE_DEVICES_ACTION,
}

val APP_FRAGMENTS = listOf(
    NavigationFragment.Home,
    NavigationFragment.Login,
    NavigationFragment.MIBCatalog,
    NavigationFragment.Network,
    NavigationFragment.Queries,
    NavigationFragment.Settings,
    NavigationFragment.Info,
    NavigationFragment.RemoveDevicesAction,
)

fun screenToFragment(
    screen: Screen?,
): NavigationFragment {
    return when (screen) {
        Screen.HOME -> NavigationFragment.Home
        Screen.LOGIN -> NavigationFragment.Login
        Screen.MIBCATALOG -> NavigationFragment.MIBCatalog
        Screen.SETTINGS -> NavigationFragment.Settings
        Screen.QUERIES -> NavigationFragment.Queries
        Screen.NETWORK -> NavigationFragment.Network
        Screen.INFO -> NavigationFragment.Info

        Screen.INFO_SECTION -> NavigationFragment.InfoSection
        Screen.DEVICE_DETAIL -> NavigationFragment.DeviceDetail
        Screen.QUERY_DETAIL -> NavigationFragment.QueryDetail

        Screen.REMOVE_DEVICES_ACTION, null -> NavigationFragment.Home
    }
}