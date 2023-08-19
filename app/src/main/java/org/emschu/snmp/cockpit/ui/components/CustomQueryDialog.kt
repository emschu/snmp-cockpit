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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Pin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.model.CustomQuery
import org.emschu.snmp.cockpit.ui.screens.ValidationRow
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.emschu.snmp.cockpit.ui.viewmodel.QueryViewModel

@Composable
fun CustomQueryDialog(
    queryViewModel: QueryViewModel,
) {
    val showDialog = queryViewModel.isDialogShown
    if (showDialog.value) {
        val customQuery = queryViewModel.currentCustomQuery.value ?: return
        val isUpdateDialog = customQuery.id > 0
        val context = LocalContext.current

        val nameValue = rememberSaveable { mutableStateOf(customQuery.name) }
        val oidValue = rememberSaveable { mutableStateOf(customQuery.oid) }
        val isSingleQuery = rememberSaveable { mutableStateOf(customQuery.isSingleQuery) }
        val isShownInDetailsTab = rememberSaveable { mutableStateOf(customQuery.isShowInDetailsTab) }
        val validationMap = remember { mutableStateMapOf<String, List<String>>() }

        AlertDialog(onDismissRequest = { queryViewModel.hideCustomQueryDialog() }, title = {
            Text(
                if (isUpdateDialog) {
                    stringResource(R.string.custom_query_edit_dialog_label)
                } else {
                    stringResource(id = R.string.custom_query_create_dialog_label)
                }, fontWeight = FontWeight.Bold
            )
        }, text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                    ToggleChip(
                        onClick = { isSingleQuery.value = !isSingleQuery.value },
                        onText = stringResource(R.string.single_query),
                        offText = stringResource(R.string.no_single_query),
                        toggleState = isSingleQuery.value,
                        fontSize = 14.sp,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    ToggleChip(
                        onClick = { isShownInDetailsTab.value = !isShownInDetailsTab.value },
                        onText = stringResource(R.string.show_in_details_tab),
                        offText = stringResource(R.string.do_not_show_in_details_tab),
                        toggleState = isShownInDetailsTab.value,
                        icon = Icons.Filled.Details,
                        fontSize = 14.sp,
                    )
                }

                Row {
                    OutlinedTextField(nameValue.value,
                                      modifier = Modifier.fillMaxWidth(),
                                      onValueChange = { nameValue.value = it },
                                      label = { Text(stringResource(R.string.custom_query_name_label)) },
                                      keyboardOptions = KeyboardOptions(
                                          keyboardType = KeyboardType.Text, autoCorrect = false,
                                          imeAction = ImeAction.Next
                                      ),
                                      keyboardActions = defaultKeyBoardActions(queryViewModel, customQuery),
                                      isError = hasFieldErrors(validationMap, CustomQueryFormFields.NAME_FIELD.key),
                                      singleLine = true,
                                      leadingIcon = {
                                          Icon(imageVector = Icons.Filled.Badge, contentDescription = "user icon")
                                      })
                }
                ValidationRow(validationMap, CustomQueryFormFields.NAME_FIELD.key)
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(oidValue.value,
                                      modifier = Modifier.fillMaxWidth(),
                                      onValueChange = { oidValue.value = it },
                                      label = { Text(stringResource(R.string.custom_query_oid)) },
                                      keyboardOptions = KeyboardOptions(
                                          keyboardType = KeyboardType.Decimal, autoCorrect = false,
                                          imeAction = ImeAction.Done
                                      ),
                                      keyboardActions = defaultKeyBoardActions(queryViewModel, customQuery),
                                      isError = hasFieldErrors(validationMap, CustomQueryFormFields.OID_FIELD.key),
                                      singleLine = true,
                                      leadingIcon = {
                                          Icon(imageVector = Icons.Filled.Pin, contentDescription = "user icon")
                                      })
                }
                ValidationRow(validationMap, CustomQueryFormFields.OID_FIELD.key)
            }
        }, confirmButton = {
            Button(onClick = {
                val currentCustomQuery = customQuery.copy(
                    name = nameValue.value,
                    oid = oidValue.value,
                    isSingleQuery = isSingleQuery.value,
                    isShowInDetailsTab = isShownInDetailsTab.value,
                )
                val customQueryForm = ValidatedForm(
                    getCustomQueryFormElements()
                )
                val formData = hashMapOf(
                    Pair(CustomQueryFormFields.NAME_FIELD.key, nameValue.value),
                    Pair(CustomQueryFormFields.OID_FIELD.key, oidValue.value),
                    Pair(CustomQueryFormFields.IS_SIMPLE_QUERY_FIELD.key, isSingleQuery.value.toString()),
                    Pair(CustomQueryFormFields.IS_SHOWN_IN_DETAILS.key, isShownInDetailsTab.value.toString()),
                )
                // this is the basic form validation
                val isValid = customQueryForm.isValid(context, formData, validationMap)
                if (!isValid) {
                    return@Button
                }
                queryViewModel.storeQuery(currentCustomQuery)
                queryViewModel.hideCustomQueryDialog()
            }) {
                Text(stringResource(R.string.btn_ok))
            }
        }, dismissButton = {
            Button(
                onClick = {
                    queryViewModel.removeCustomQuery(customQuery)
                    queryViewModel.hideCustomQueryDialog()
                },
                enabled = isUpdateDialog,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text(stringResource(R.string.delete_label))
            }
            Button(onClick = {
                queryViewModel.hideCustomQueryDialog()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        )
    }
}

@Composable
private fun defaultKeyBoardActions(queryViewModel: QueryViewModel, customQuery: CustomQuery): KeyboardActions {
    val focusManager = LocalFocusManager.current
    return KeyboardActions(onNext = {
        focusManager.moveFocus(FocusDirection.Down)
    }, onDone = {
        queryViewModel.storeQuery(customQuery)
        queryViewModel.hideCustomQueryDialog()
    })
}

@Preview(widthDp = 500)
@Composable
fun CustomQueryDialogPreview() {
    CockpitTheme(darkTheme = false) {
        val queryViewModel = viewModel<QueryViewModel>()
        queryViewModel.showCreateNewQueryDialog()
        CustomQueryDialog(
            queryViewModel = queryViewModel
        )
    }
}