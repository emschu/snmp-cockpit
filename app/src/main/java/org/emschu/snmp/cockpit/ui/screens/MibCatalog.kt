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

package org.emschu.snmp.cockpit.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.emschu.android.treeview.TreeItemNodeFlat
import org.emschu.android.treeview.TreeView
import org.emschu.android.treeview.TreeViewModel
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.snmp.MibCatalogArchiveManager
import org.emschu.snmp.cockpit.snmp.MibCatalogLoader.Companion.isTreeNodeDeep
import org.emschu.snmp.cockpit.snmp.MibCatalogManager
import org.emschu.snmp.cockpit.snmp.json.MibCatalog
import org.emschu.snmp.cockpit.ui.makeNotification
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterialApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MIBCatalogView(
    mainViewModel: MainViewModel,
    bottomSheetScaffoldState: ModalBottomSheetState,
    treeViewModel: TreeViewModel,
) {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val clickHandler: (TreeItemNodeFlat) -> Unit = {
        if (it.isQueryable() && isTreeNodeDeep(it.nodeLabel)) {
            mainViewModel.triggerBottomSheet(scope, it.nodeContent, bottomSheetScaffoldState)
        }
    }
    TreeView(
        treeViewModel,
        listState = lazyListState,
        onLongClick = clickHandler,
        onDoubleClick = clickHandler,
        onClick = {},
    )
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), onResult = {
        Log.d(LOG_TAG, "received result from file dialog, $it")
        if (it == null) {
            return@rememberLauncherForActivityResult
        }

        val fm = MibCatalogArchiveManager(context, it)
        val mcm = MibCatalogManager(PreferenceManager.getDefaultSharedPreferences(context))

        if (mcm.isDuplicate(fm.archiveName)) {
            makeNotification(context, "", R.string.mib_catalog_duplicate_toast_message)
        } else if (fm.isArchiveValid) {
            val success: Boolean = fm.unpackZip()
            if (success) {
                Log.i(
                    LOG_TAG, java.lang.String.format("successfully imported '%s'", fm.archiveName)
                )
                val newCatalog = MibCatalog(fm.archiveName)
                mcm.mibCatalog.add(newCatalog)
                mcm.storeCatalog()
                mcm.activateCatalog(fm.archiveName)
                Log.i(LOG_TAG, "added new MIB catalog and activated it")
                makeNotification(
                    context,
                    "",
                    context.getString(R.string.new_mib_catalog_created_toast_message)
                        .format(fm.archiveName)
                )
            } else {
                Log.w(LOG_TAG, "Import of archive '%s' was not possible".format(fm.archiveName))
                makeNotification(context, "", R.string.error_importing_mib_catalog_archive)
            }
        } else {
            Log.w(LOG_TAG, java.lang.String.format("archive '%s' is NOT valid!", fm.archiveName))
            makeNotification(context, "", R.string.invalid_mib_catalog_archive_toast_message)
        }
    })

    if (mainViewModel.showImportCatalogDialog.value) {
        AlertDialog(
            onDismissRequest = { mainViewModel.hideMibCatalogImportDialog() },
            title = {
                Column {
                    Row {
                        Text(
                            stringResource(id = R.string.mib_catalog_import_process),
                            fontSize = MaterialTheme.typography.h6.fontSize
                        )
                    }
                    Row {
                        Text(stringResource(id = R.string.mib_catalog_import_intent_extra))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        mainViewModel.hideMibCatalogImportDialog()
                        launcher.launch("application/zip")
                    }
                }) {
                    Text(stringResource(id = R.string.btn_ok))
                }
            },
            dismissButton = {
                Button(onClick = {
                    mainViewModel.hideMibCatalogImportDialog()
                }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
        )
    }
}

private const val LOG_TAG = "org.emschu.snmp.cockpit.ui.screens.MibCatalog"