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

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.twotone.Sensors
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.emschu.android.treeview.TreeViewModel
import org.emschu.snmp.cockpit.BuildConfig
import org.emschu.snmp.cockpit.MainActivity
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.SettingsActivity
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.network.NetworkDisplayInfo
import org.emschu.snmp.cockpit.network.WifiNetworkManager
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.snmp.DeviceManager
import org.emschu.snmp.cockpit.ui.screens.AppInformationView
import org.emschu.snmp.cockpit.ui.screens.CustomQueryView
import org.emschu.snmp.cockpit.ui.screens.DeviceDetailView
import org.emschu.snmp.cockpit.ui.screens.DeviceListView
import org.emschu.snmp.cockpit.ui.screens.InfoScreen
import org.emschu.snmp.cockpit.ui.screens.InfoSectionDetailView
import org.emschu.snmp.cockpit.ui.screens.LoginView
import org.emschu.snmp.cockpit.ui.screens.MIBCatalogView
import org.emschu.snmp.cockpit.ui.screens.NetworkDetailsView
import org.emschu.snmp.cockpit.ui.screens.QueryDetailView
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.theme.SignalGreenYellow
import org.emschu.snmp.cockpit.ui.viewmodel.DeviceDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.LoginViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryViewModel

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi // at least because of "onClick" of Card
@Composable
fun NavigationDrawer(
    mainViewModel: MainViewModel = viewModel(),
    deviceDetailViewModel: DeviceDetailViewModel = viewModel(),
    queryDetailViewModel: QueryDetailViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel(),
    treeViewModel: TreeViewModel = viewModel(),
    queryViewModel: QueryViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val modalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentScreen = mainViewModel.currentScreen.value
    val isSpinnerActivated = mainViewModel.spinner.value
    val areAllDevicesOnline = DeviceManager.onlineState
    val isConnected = SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.observeAsState()
    val showHasNetworkProblems = isConnected.value != true && currentScreen in listOf(
        Screen.HOME, Screen.LOGIN, Screen.DEVICE_DETAIL, Screen.QUERY_DETAIL, Screen.NETWORK
    )

    // enable navigation of navigator
    LaunchedEffect(key1 = currentScreen, block = {
        mainViewModel.navigator.sharedFlow.onEach {
            if (!singlePageFragments.contains(it)) {
                mainViewModel.navigateTo(navController, it)
            }
        }.launchIn(this)
    })

    // use a modal bottom sheet layout(=wrapper) which wraps a scaffold (=app + drawer + ...)
    ModalBottomSheetLayout(
        sheetContent = {
            BottomSheetContent(mainViewModel, navController, modalBottomSheetState)
        },
        sheetElevation = 2.dp,
        sheetBackgroundColor = MaterialTheme.colors.background,
        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        sheetState = modalBottomSheetState,
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                if (currentScreen == Screen.HOME) {
                    FloatingActionButton(onClick = {
                        mainViewModel.navigator.navigateTo(Screen.LOGIN)
                    }, modifier = Modifier.padding(bottom = 32.dp)) {
                        Icon(Icons.Rounded.Login, "login_fab")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            drawerBackgroundColor = MaterialTheme.colors.primary,
            drawerContent = {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    DrawerHeader()

                    DrawerDivider()

                    SideEffect {
                        WifiNetworkManager.refresh()
                    }

                    val selectedDevice = mainViewModel.currentDeviceScreen.value
                    val selectedScreen: NavigationFragment = screenToFragment(currentScreen)
                    Drawer(
                        selectedScreen = selectedScreen, onMenuSelected = { screen: Screen ->
                            scope.launch {
                                scaffoldState.drawerState.close()
                            }
                            APP_FRAGMENTS.filter { it.id == screen }.forEach {
                                if (it.id == Screen.SETTINGS) {
                                    scope.launch {
                                        val intent = Intent(context, SettingsActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                } else {
                                    mainViewModel.navigator.navigateTo(it.id)
                                }
                            }
                        }, selectedDevice = selectedDevice,
                        onDeviceSelected = { deviceId ->
                            scope.launch {
                                scaffoldState.drawerState.close()
                            }
                            mainViewModel.navigateToDeviceDetails(navController, deviceId)
                        })
                }
            },
            content = { padding ->
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(padding)
                ) {
                    Row {
                        if (isSpinnerActivated) {
                            Column {
                                val progressInfo = mainViewModel.progressText.value
                                val connectionText = "%.0f %%".format(mainViewModel.progressValue.value * 100)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colors.primary),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (mainViewModel.progressValue.value > 0) {
                                        Text(
                                            connectionText,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                            color = MaterialTheme.colors.onPrimary,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Left,
                                        )
                                    }
                                    if (progressInfo.isNotBlank()) {
                                        Text(
                                            stringResource(id = R.string.testing) + " $progressInfo",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                            textAlign = TextAlign.Right,
                                            color = MaterialTheme.colors.onPrimary,
                                            fontSize = 14.sp,
                                            maxLines = 2
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    mainViewModel.progressValue.value, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                )
                            }
                        } else if (showHasNetworkProblems) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colors.primary),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Warning, contentDescription = "connection_error")
                                    Text(
                                        text = stringResource(id = R.string.no_wifi_available),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                        color = MaterialTheme.colors.onPrimary,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Left
                                    )
                                }
                            }
                        } else if (areAllDevicesOnline.value.not()) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colors.primary),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    NotOnlineChip(stringResource(R.string.snmp_connection_problems))
                                }
                            }
                        } else {
                            Column {}
                        }
                    }

                    Row {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                DeviceListView(navController, mainViewModel);
                                FootComponent(mainViewModel)
                            }
                            composable("login") {
                                LoginView(loginViewModel, mainViewModel)
                                FootComponent(mainViewModel)
                            }
                            composable("mibcatalog") {
                                MIBCatalogView(mainViewModel, modalBottomSheetState, treeViewModel)
                                FootComponent(mainViewModel)
                            }
                            composable("queries") {
                                CustomQueryView(
                                    mainViewModel, queryViewModel, modalBottomSheetState
                                );
                                FootComponent(mainViewModel)
                            }
                            composable("network") {
                                SideEffect {
                                    WifiNetworkManager.refresh()
                                }
                                NetworkDetailsView(mainViewModel);
                                FootComponent(mainViewModel)
                            }
                            composable("info") {
                                AppInformationView(
                                    navController, mainViewModel
                                )
                                FootComponent(mainViewModel)
                            }
                            composable(
                                "device_detail/{deviceId}",
                                arguments = listOf(navArgument("deviceId") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                                DeviceManager.getDevice(deviceId)?.let {
                                    deviceDetailViewModel.addLatestDevice(it)
                                    DeviceDetailView(
                                        mainViewModel,
                                        navController,
                                        it,
                                        deviceDetailViewModel
                                    )
                                }
                                FootComponent(mainViewModel)
                            }
                            composable(
                                "query_detail/{deviceId}/{query}",
                                arguments = listOf(navArgument("deviceId") {
                                    type = NavType.StringType
                                }, navArgument("query") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                                val oidQuery = backStackEntry.arguments?.getString("query") ?: ""
                                DeviceManager.getDevice(deviceId)?.let {
                                    queryDetailViewModel.addLatestDevice(it)
                                    queryDetailViewModel.addCurrentQuery(oidQuery)
                                    QueryDetailView(queryDetailViewModel)
                                }
                                FootComponent(mainViewModel)
                            }
                            composable(
                                "info_section/{sectionId}",
                                arguments = listOf(navArgument("sectionId") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                InfoSectionDetailView(
                                    InfoScreen.valueOf(backStackEntry.arguments?.getString("sectionId") ?: ""),
                                    mainViewModel,
                                    navController,
                                )
                                FootComponent(mainViewModel)
                            }
                        }
                    }
                }

                when (currentScreen) {
                    Screen.HOME -> mainViewModel.navigator.navigateTo(Screen.HOME)
                    Screen.LOGIN -> mainViewModel.navigator.navigateTo(Screen.LOGIN)
                    Screen.QUERIES -> mainViewModel.navigator.navigateTo(Screen.QUERIES)
                    Screen.NETWORK -> mainViewModel.navigator.navigateTo(Screen.NETWORK)
                    Screen.INFO -> mainViewModel.navigator.navigateTo(Screen.INFO)
                    Screen.MIBCATALOG -> mainViewModel.navigator.navigateTo(Screen.MIBCATALOG)
                    Screen.REMOVE_DEVICES_ACTION -> mainViewModel.showRemoveDeviceDialog.postValue(true)

                    else -> {}
                }

                var isFirstBackTap = true
                BackHandler(enabled = true) {
                    // central place of the application to define the BackHandler logic + back navigation logic
                    if (scaffoldState.drawerState.isOpen) {
                        scope.launch {
                            scaffoldState.drawerState.close()
                        }
                        isFirstBackTap = true
                        return@BackHandler
                    }
                    if (modalBottomSheetState.isVisible) {
                        scope.launch {
                            modalBottomSheetState.hide()
                        }
                        isFirstBackTap = true
                        return@BackHandler
                    }
                    when (mainViewModel.currentScreen.value) {
                        Screen.HOME -> {
                            // implementation of double-tap quit feature
                            if (!isFirstBackTap) {
                                (context as MainActivity).finish()
                            }
                            isFirstBackTap = false
                            Toast.makeText(
                                context, context.getString(R.string.press_back_again_to_quit), Toast.LENGTH_SHORT
                            ).show()
                        }

                        Screen.INFO_SECTION -> {
                            mainViewModel.navigator.navigateTo(Screen.INFO)
                            isFirstBackTap = true
                        }

                        Screen.QUERY_DETAIL -> {
                            mainViewModel.navigator.navigateTo(mainViewModel.navigator.lastScreen ?: Screen.MIBCATALOG)
                            isFirstBackTap = true
                        }

                        else -> {
                            mainViewModel.navigator.navigateTo(Screen.HOME)
                            isFirstBackTap = true
                        }
                    }
                }
            },
            topBar = {
                TopAppBarLayout(
                    mainViewModel = mainViewModel,
                    deviceDetailViewModel = deviceDetailViewModel,
                    loginViewModel = loginViewModel,
                    queryViewModel = queryViewModel,
                    queryDetailViewModel = queryDetailViewModel,
                    scope = scope,
                    scaffoldState = scaffoldState,
                    isSpinnerActivated = isSpinnerActivated,
                    currentScreen = currentScreen,
                    navController = navController,
                )
            },
            drawerGesturesEnabled = true,
        )
    }
}

@Composable
fun Drawer(
    modifier: Modifier = Modifier,
    selectedDevice: String = "",
    selectedScreen: NavigationFragment,
    onMenuSelected: ((screen: Screen) -> Unit)? = null,
    onDeviceSelected: ((deviceId: String) -> Unit)? = null,
) {
    Column(
        modifier
            .background(MaterialTheme.colors.primary)
            .fillMaxSize()
    ) {
        if (SnmpCockpitApp.deviceManager.deviceConnectionList.isNotEmpty()) {
            SnmpCockpitApp.deviceManager.deviceConnectionList.take(3).forEach {
                DrawerDeviceItemRow(
                    titleLabel = it.deviceConfiguration.getListLabel(null),
                    deviceConfig = it.deviceConfiguration,
                    onDeviceSelected = onDeviceSelected,
                    isActive = selectedDevice == it.deviceConfiguration.uniqueDeviceId
                )
            }
            DrawerDivider()
        }

        // display all known fragments
        APP_FRAGMENTS.filter { !it.isAction }.forEach { screen ->
            when (screen) {
                NavigationFragment.Home -> {
                    DrawerItemRow(screen, selectedScreen, onMenuSelected, title = {
                        val deviceList = SnmpCockpitApp.deviceManager.deviceConnectionList
                        Column(horizontalAlignment = Alignment.Start) {
                            if (deviceList.isEmpty()) {
                                Text(
                                    text = stringResource(id = screen.title),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (screen.id == selectedScreen.id) Color.White else MaterialTheme.colors.onPrimary,
                                )
                            } else {
                                BadgedBox(
                                    badge = {
                                        Badge(contentColor = Color.Black, backgroundColor = SignalGreenYellow) {
                                            Text(
                                                "" + deviceList.size
                                            )
                                        }
                                    },
                                ) {
                                    Text(
                                        text = stringResource(id = screen.title),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (screen.id == selectedScreen.id) Color.White else MaterialTheme.colors.onPrimary,
                                    )
                                }
                            }

                        }
                    })
                }

                NavigationFragment.Login -> {
                    // do not display login in app menu
                }

                else -> {
                    DrawerItemRow(screen, selectedScreen, onMenuSelected)
                }
            }
        }

        DrawerDivider()

        APP_FRAGMENTS.filter { it.isAction }.forEach { screen ->
            if (screen.id == Screen.REMOVE_DEVICES_ACTION) {
                if (SnmpCockpitApp.deviceManager.deviceConnectionList.isNotEmpty()) {
                    DrawerItemRow(screen, selectedScreen, onMenuSelected)
                }
            } else {
                DrawerItemRow(screen, selectedScreen, onMenuSelected)
            }
        }
    }
    // print version
    Box(contentAlignment = Alignment.BottomStart, modifier = Modifier.fillMaxSize()) {
        val versionSuffix = if (BuildConfig.BUILD_TYPE != "release") {
            "-" + BuildConfig.BUILD_TYPE
        } else {
            ""
        }
        Text(
            "${BuildConfig.BUILD_APP_VERSION_STRING}-${BuildConfig.VERSION_NAME}$versionSuffix",
            color = MaterialTheme.colors.onPrimary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DrawerDivider() {
    Divider(thickness = 3.dp, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
}

@Composable
private fun DrawerItemRow(
    screen: NavigationFragment,
    selectedScreen: NavigationFragment,
    onMenuSelected: ((screen: Screen) -> Unit)?,
    title: @Composable () -> Unit = {
        Text(
            text = stringResource(id = screen.title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (screen.id == selectedScreen.id) Color.White else MaterialTheme.colors.onPrimary,
        )
    },
) {
    Row(
        content = {
            Column(Modifier.padding(end = 8.dp)) {
                screen.icon()
            }
            Column {
                title()
            }
        }, modifier = Modifier
            .background(
                color = if (screen.id == selectedScreen.id) MaterialTheme.colors.secondary else MaterialTheme.colors.primary,
                shape = RoundedCornerShape(2.dp)
            )
            .fillMaxWidth()
            .clickable(onClick = {
                onMenuSelected?.invoke(screen.id)
            })
            .padding(top = 8.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun DrawerDeviceItemRow(
    titleLabel: String,
    deviceConfig: DeviceConfiguration,
    onDeviceSelected: ((deviceId: String) -> Unit)?,
    isActive: Boolean = false,
) {
    Row(
        content = {
            Column {
                Icon(Icons.TwoTone.Sensors, "device connection icon")
            }

            if (!deviceConfig.isOnline) {
                Column {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "device ${deviceConfig.uniqueDeviceId} has problems"
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = titleLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else MaterialTheme.colors.onPrimary,
                )
            }

        },
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (isActive) MaterialTheme.colors.secondary else MaterialTheme.colors.primary,
                shape = RoundedCornerShape(2.dp)
            )
            .fillMaxWidth()
            .clickable(onClick = {
                onDeviceSelected?.invoke(deviceConfig.uniqueDeviceId)
            })
            .padding(top = 4.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
    )
}


@Composable
fun DrawerHeader() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(140.dp, 240.dp)
            .background(MaterialTheme.colors.primaryVariant)
            .scrollable(
                enabled = true,
                state = scrollState,
                orientation = Orientation.Vertical,
            ),
        content = {
            Column {
                Row {
                    AppLogoWithText()
                }
                Row {
                    val infoState by WifiNetworkManager.displayInfoState.collectAsState()
                    DrawerNetworkDetails(infoState)
                }
            }
        }, verticalArrangement = Arrangement.Bottom
    )
}

@Composable
fun DrawerNetworkDetails(infoState: NetworkDisplayInfo) {
    val ssidLabel = infoState.currentSSIDLabel
    val ipAddressLabel = infoState.ipAddressLabel
    val ip6AddressLabel = infoState.ipv6AddressLabel

    Column(
        Modifier
            .padding(top = 4.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        if (!ssidLabel.endsWith("-")) {
            Row {
                DrawerNetworkInfoText(ssidLabel)
            }
        }
        if (!ipAddressLabel.endsWith("-")) {
            Row {
                DrawerNetworkInfoText(ipAddressLabel)
            }
        }
        if (!ip6AddressLabel.endsWith("-")) {
            Row(Modifier.padding(end = 5.dp)) {
                DrawerNetworkInfoText(ip6AddressLabel)
            }
        }
    }
}

@Composable
private fun DrawerNetworkInfoText(text: String, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        fontSize = MaterialTheme.typography.subtitle2.fontSize,
        color = contentColorFor(backgroundColor = MaterialTheme.colors.primary),
        overflow = TextOverflow.Visible,
        textAlign = textAlign,
    )
}

/**
 * This composable is part of every screen
 */
@Composable
private fun FootComponent(viewModel: MainViewModel) {
    val showDialog = viewModel.showRemoveDeviceDialog.observeAsState()
    if (showDialog.value != false) {
        AlertDialog(text = {
            Text(
                stringResource(R.string.remove_all_connections_dialog),
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.h6.fontSize
            )
        }, onDismissRequest = { viewModel.showRemoveDeviceDialog.postValue(false) }, confirmButton = {
            Button(onClick = {
                viewModel.showRemoveDeviceDialog.postValue(false)
                viewModel.removeAllDevices()
            }) {
                Text(stringResource(id = R.string.btn_ok))
            }
        }, dismissButton = {
            Button(onClick = {
                viewModel.showRemoveDeviceDialog.postValue(false)
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }
}

@Preview
@Composable
private fun DrawerPreviewLight() {
    val selectedScreen: NavigationFragment = screenToFragment(Screen.HOME)

    CockpitTheme(darkTheme = false) {
        Column {
            Row {
                DrawerHeader()
            }
            Row {
                Drawer(selectedScreen = selectedScreen, onMenuSelected = { })
            }
        }
    }
}

@Preview
@Composable
private fun DrawerPreviewDark() {
    val selectedScreen = remember { screenToFragment(Screen.HOME) }

    CockpitTheme(darkTheme = true) {
        Column {
            Row {
                DrawerHeader()
            }
            Row(modifier = Modifier.padding(16.dp)) {
                Drawer(selectedScreen = selectedScreen, onMenuSelected = { })
            }
        }
    }
}
