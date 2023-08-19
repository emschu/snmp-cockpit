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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import org.emschu.snmp.cockpit.ui.components.LoginFormFields
import org.emschu.snmp.cockpit.ui.components.ToggleChip
import org.emschu.snmp.cockpit.ui.components.hasFieldErrors
import org.emschu.snmp.cockpit.ui.viewmodel.LoginViewModel
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel
import org.emschu.snmp.cockpit.R as R1


@Composable
fun LoginView(
    loginViewModel: LoginViewModel, mainViewModel: MainViewModel,
) {
    val isIpv6 = loginViewModel.isIpv6.observeAsState()
    val isSnmpv3 = loginViewModel.isSnmpv3.observeAsState()
    val deviceQrCode = loginViewModel.scannedEndpoint
    val validationMap = loginViewModel.validationMapMutable.toMap()

    if (deviceQrCode.value != null) {
        loginViewModel.populateForm(deviceQrCode.value!!)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(0.7f), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToggleChip(
                    onClick = { loginViewModel.isSnmpv3.postValue(it) },
                    onText = "SNMPv3",
                    isSnmpv3.value ?: false,
                    when (loginViewModel.snmpVersion) {
                        DeviceConfiguration.SNMP_VERSION.v1 -> "SNMPv1"
                        DeviceConfiguration.SNMP_VERSION.v2c -> "SNMPv2c"
                        else -> "SNMPv3"
                    }
                )
                ToggleChip(
                    onClick = {
                        loginViewModel.isIpv6.postValue(it)
                    }, onText = "IPv6", isIpv6.value ?: false, "IPv4", icon = Icons.Filled.Speed
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        NetworkAddressFieldGroup(loginViewModel, mainViewModel, validationMap)

        Spacer(modifier = Modifier.height(16.dp))

        ShowVersionSpecificFields(loginViewModel, isSnmpv3, mainViewModel, validationMap)
    }
}

@Composable
private fun ShowVersionSpecificFields(
    loginViewModel: LoginViewModel,
    isSnmpv3: State<Boolean?>,
    mainViewModel: MainViewModel,
    validationMap: Map<String, List<String>>,
) {
    var communityValue by remember { mutableStateOf(loginViewModel.community.value) }
    var userValue by remember { mutableStateOf(loginViewModel.user.value) }
    var passwordValue by remember { mutableStateOf(loginViewModel.password.value) }
    var encKeyValue by remember { mutableStateOf(loginViewModel.key.value) }

    // version specific fields
    if (isSnmpv3.value == true) {
        Row {
            OutlinedTextField(
                userValue ?: "",
                onValueChange = {
                    userValue = it
                    loginViewModel.user.postValue(it)
                },
                label = { Text(stringResource(id = R1.string.user_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text, autoCorrect = false, imeAction = ImeAction.Next
                ),
                keyboardActions = defaultKeyBoardActions(loginViewModel, mainViewModel),
                isError = hasFieldErrors(validationMap, LoginFormFields.USER_FIELD.key),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person, contentDescription = "user icon"
                    )
                },
            )
        }
        ValidationRow(validationMap, LoginFormFields.USER_FIELD.key)
        Spacer(modifier = Modifier.height(4.dp))

        var isPasswordVisible by remember { mutableStateOf(false) }
        Row {
            OutlinedTextField(
                passwordValue ?: "",
                onValueChange = {
                    passwordValue = it
                    loginViewModel.password.postValue(it)
                },
                label = { Text(stringResource(id = R1.string.password_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Next
                ),
                isError = hasFieldErrors(validationMap, LoginFormFields.PW_FIELD.key),
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = {
                        isPasswordVisible = !isPasswordVisible
                    }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff, contentDescription = "Password Visibility"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            )
        }
        ValidationRow(validationMap, LoginFormFields.PW_FIELD.key)
        Spacer(modifier = Modifier.height(4.dp))

        var isKeyVisible by remember { mutableStateOf(false) }
        Row {
            OutlinedTextField(
                encKeyValue ?: "",
                onValueChange = {
                    encKeyValue = it
                    loginViewModel.key.postValue(it)
                },
                label = { Text(stringResource(id = R1.string.snmpv3_key)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Done
                ),
                isError = hasFieldErrors(validationMap, LoginFormFields.ENC_FIELD.key),
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = {
                        isKeyVisible = !isKeyVisible
                    }) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff, contentDescription = "Key Visibility"
                        )
                    }
                },
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            )
        }
        ValidationRow(validationMap, LoginFormFields.ENC_FIELD.key)

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Column {
                Text(stringResource(id = R1.string.auth_protocol_label) + " ", fontWeight = FontWeight.Bold)
            }
            Column {
                Text(stringResource(org.emschu.snmp.cockpit.R.string.try_all))
            }
        }
        Row {
            Column {
                Text(stringResource(id = R1.string.security_protocol_label) + " ", fontWeight = FontWeight.Bold)
            }
            Column {
                Text(stringResource(org.emschu.snmp.cockpit.R.string.try_all))
            }
        }
    } else {
        Row {
            OutlinedTextField(
                communityValue ?: "",
                onValueChange = {
                    communityValue = it
                    loginViewModel.community.postValue(it)
                },
                label = { Text(stringResource(id = R1.string.community_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text, autoCorrect = false, imeAction = ImeAction.Done
                ),
                keyboardActions = defaultKeyBoardActions(loginViewModel, mainViewModel),
                isError = hasFieldErrors(validationMap, LoginFormFields.COMMUNITY_FIELD.key),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = "user icon")
                }
            )
        }
        ValidationRow(validationMap, LoginFormFields.COMMUNITY_FIELD.key)
    }
}

@Composable
private fun NetworkAddressFieldGroup(
    loginViewModel: LoginViewModel,
    mainViewModel: MainViewModel,
    validationMap: Map<String, List<String>>,
) {
    var hostValue by remember { mutableStateOf(loginViewModel.host.value) }
    var portValue by remember { mutableStateOf(loginViewModel.port.value) }

    Row {
        OutlinedTextField(
            hostValue ?: "",
            onValueChange = {
                hostValue = it
                loginViewModel.host.postValue(it)
            },
            label = { Text(stringResource(R1.string.host_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = defaultKeyBoardActions(loginViewModel, mainViewModel),
            isError = hasFieldErrors(validationMap, LoginFormFields.HOST_FIELD.key),
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Filled.LocationSearching, contentDescription = "user icon")
            })
    }
    ValidationRow(validationMap, LoginFormFields.HOST_FIELD.key)
    Spacer(modifier = Modifier.height(4.dp))

    Row {
        OutlinedTextField(
            portValue ?: "",
            onValueChange = {
                portValue = it
                loginViewModel.port.postValue(it)
            },
            label = { Text(stringResource(R1.string.port_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, autoCorrect = false, imeAction = ImeAction.Next
            ),
            keyboardActions = defaultKeyBoardActions(loginViewModel, mainViewModel),
            isError = hasFieldErrors(validationMap, LoginFormFields.PORT_FIELD.key),
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Filled.ModeOfTravel, contentDescription = "user icon")
            })
    }
    ValidationRow(validationMap, LoginFormFields.PORT_FIELD.key)
}

@Composable
fun ValidationRow(
    validationMap: Map<String, List<String>>, validationKey: String,
) {
    Row(horizontalArrangement = Arrangement.Center) {
        if ((validationMap[validationKey]?.size ?: 0) > 0) {
            val errorStringList = validationMap.get(validationKey) ?: emptyList()
            Column {
                errorStringList.forEach {
                    Row {
                        Text(it, color = MaterialTheme.colors.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun defaultKeyBoardActions(loginViewModel: LoginViewModel, mainViewModel: MainViewModel): KeyboardActions {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    return KeyboardActions(onNext = {
        focusManager.moveFocus(FocusDirection.Down)
    }, onDone = {
        loginViewModel.login(context, mainViewModel)
    })
}

@Preview(widthDp = 500)
@Composable
private fun DefaultPreview() {
    LoginView(viewModel(), viewModel())
}
