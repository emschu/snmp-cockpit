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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.emschu.android.treeview.TreeItemNodeFlat
import org.emschu.android.treeview.TreeNode
import org.emschu.android.treeview.TreeViewModel
import org.emschu.android.treeview.traverseTreeToList
import org.emschu.snmp.cockpit.network.WifiNetworkManager
import org.emschu.snmp.cockpit.snmp.MibCatalogLoader
import org.emschu.snmp.cockpit.tasks.DeviceAddTask
import org.emschu.snmp.cockpit.tasks.SystemQueryTask
import org.emschu.snmp.cockpit.ui.components.NavigationDrawer
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.makeNotification
import org.emschu.snmp.cockpit.ui.screens.CockpitAppIntro
import org.emschu.snmp.cockpit.ui.sources.AssetContentSource
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.viewmodel.DeviceDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.LoginViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryDetailViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.QueryViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val queryViewModel: QueryViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private val deviceDetailViewModel: DeviceDetailViewModel by viewModels()
    private val queryDetailViewModel: QueryDetailViewModel by viewModels()
    private val treeViewModel: TreeViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private var periodicJob: Job? = null
    private lateinit var workManager: WorkManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGrantedMap: Map<String, Boolean> ->
        if (isGrantedMap.isEmpty()) {
            if (!hasAllPermissions()) {
                Toast.makeText(this, getString(R.string.insufficient_permissions_toast), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.workManager = WorkManager.getInstance(this)

        TrafficStats.setThreadStatsTag(14242)

        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
            try {
                Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                    .invoke(null, true)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            AssetContentSource.initialLoading(this@MainActivity)
            SnmpCockpitApp.cockpitStateManager.networkAvailabilityObservable.postValue(true)
            WifiNetworkManager.initialize()
            WifiNetworkManager.refresh()

            // finally load mib catalog
            val tree = TreeNode(null, "1 - iso")
            val mibCatalogLoader = MibCatalogLoader()
            mibCatalogLoader.loadMibTree(this@MainActivity, tree)

            // todo ensure these exist!
            val itemList = mutableListOf<TreeItemNodeFlat>()
            traverseTreeToList(tree, 0, itemList)
            treeViewModel.loadList(itemList)

            Log.i("MainActivity", "MIB Catalog loaded")
        }

        setContent {
            CockpitTheme {
                MainView(
                    viewModel = mainViewModel,
                    deviceDetailViewModel = deviceDetailViewModel,
                    queryDetailViewModel = queryDetailViewModel,
                    loginViewModel = loginViewModel,
                    treeViewModel = treeViewModel,
                    queryViewModel = queryViewModel
                )
            }
        }

        initPermissions()

        initJobs()

        registerHandlerForDeviceAddTask()

        handleWelcomeDialogs()
    }

    private fun registerHandlerForDeviceAddTask() {
        val testCount = SnmpCockpitApp.snmpManager.totalConnectionTestCount
        var isFirst = true
        workManager.getWorkInfosForUniqueWorkLiveData(DEVICE_ADD_TASK_WORK_NAME)
            .observe(this) { workInfoList ->
                if (isFirst) {
                    // always skip first call, because the first call is
                    // either null (e.g. no app execution before) or filled with latest - (mostly) already processed - worker result
                    isFirst = false
                    return@observe
                }
                if (workInfoList.isNullOrEmpty()) {
                    return@observe
                }
                val workInfo = workInfoList[0]
                if (workInfoList.size > 1) {
                    Log.d("MainActivity", "More than one workInfo item! Only checking first.")
                }

                if (workInfo.state.isFinished) {
                    mainViewModel.endSpinner()

                    // handle success/error/cancel cases (NOTE: workInfo is already finished)
                    if (workInfo.state in arrayOf(
                            WorkInfo.State.SUCCEEDED,
                            WorkInfo.State.CANCELLED,
                            WorkInfo.State.FAILED
                        )
                    ) {
                        Log.i("LoginComponent", "Finished connection test state: {${workInfo.state}}")
                        if (workInfo.state == WorkInfo.State.FAILED) {
                            val doesExist = workInfo.outputData.getBoolean(DeviceAddTask.OUTPUT_DOES_EXIST, false)
                            if (doesExist) {
                                makeNotification(
                                    this@MainActivity, "", R.string.connection_already_exists
                                )
                            }
                            makeNotification(
                                this@MainActivity, "", R.string.connect_to_device_toast_failed
                            )
                        } else if (workInfo.state == WorkInfo.State.CANCELLED) {
                            makeNotification(
                                this@MainActivity, "", R.string.connect_to_device_toast_cancelled
                            )
                        } else {
                            // success
                            CoroutineScope(Dispatchers.Default).launch {
                                mainViewModel.navigator.navigateTo(Screen.HOME)
                            }
                        }
                    }
                } else {
                    // handling progress update
                    val currentProgress = workInfo.progress.getInt(DeviceAddTask.PROGRESS_CURRENT_NUM, 0)
                    mainViewModel.progressValue.value = currentProgress.toFloat() / testCount
                    mainViewModel.progressText.value =
                        workInfo.progress.getString(DeviceAddTask.PROGRESS_CURRENT_COMBINATION) ?: ""
                }
            }
    }

    private fun initPermissions() {
        if (SnmpCockpitApp.preferenceManager().isWelcomeScreenShown) {
            return
        }
        when {
            hasAllPermissions() -> {
                Log.d("MainActivity", "All required permissions available")
            }

            else -> {
                requestPermissionLauncher.launch(requiredPermissions())
            }
        }
        if (BuildConfig.DEBUG) {
            requiredPermissions().forEach { permission ->
                if (shouldShowRequestPermissionRationale(permission)) {
                    Toast.makeText(this@MainActivity, "Missing Permission: $permission", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun handleWelcomeDialogs() {
        if (!SnmpCockpitApp.preferenceManager().isWelcomeScreenShown) {
            val intent = Intent(this.baseContext, CockpitAppIntro::class.java)
            startActivity(intent)
            SnmpCockpitApp.preferenceManager().setWelcomeScreenShown()
        }
    }

    private fun initJobs() {
        if (SnmpCockpitApp.preferenceManager().isPeriodicUpdateEnabled) {
            val secs = SnmpCockpitApp.preferenceManager().uiUpdateSeconds.toLong()
            if (secs > 10) {
                val inputData = Data.Builder()
                    .putBoolean(SystemQueryTask.DATA_KEY_CACHE_ENABLED, false)
                    .build()

                this.periodicJob = CoroutineScope(Dispatchers.IO).launch {
                    while (true) {
                        val periodicRefreshJob = OneTimeWorkRequest.Builder(SystemQueryTask::class.java)
                            .setInitialDelay(0, TimeUnit.SECONDS)
                            .setInputData(inputData)
                            .addTag("PeriodicQuery")
                            .build()
                        workManager.beginWith(periodicRefreshJob).enqueue()
                        delay(secs * 1000)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        this.periodicJob?.cancel()

        SnmpCockpitApp.dbHelper().close()

        super.onDestroy()
    }

    companion object {
        const val DEVICE_ADD_TASK_WORK_NAME = "device_add_task"
        const val DEVICE_UPDATE_TASK_WORK_NAME = "device_update_task"

        fun requiredPermissions() = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
        )

        private fun hasAllPermissions(): Boolean {
            requiredPermissions().forEach { permission ->
                if (ContextCompat.checkSelfPermission(
                        SnmpCockpitApp.context!!, permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }
    }
}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun MainView(
    viewModel: MainViewModel,
    deviceDetailViewModel: DeviceDetailViewModel,
    queryDetailViewModel: QueryDetailViewModel,
    loginViewModel: LoginViewModel,
    treeViewModel: TreeViewModel,
    queryViewModel: QueryViewModel,
) {
    NavigationDrawer(
        viewModel, deviceDetailViewModel, queryDetailViewModel, loginViewModel, treeViewModel, queryViewModel
    )
}