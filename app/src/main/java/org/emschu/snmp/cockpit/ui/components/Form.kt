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

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.apache.commons.validator.routines.InetAddressValidator
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.snmp.OidValidator

// helper function to get a conditional validation form instance
fun getLoginFormElements(isSnmpv3: Boolean, isIpv6: Boolean): Map<String, Array<FormValidator>> {
    return HashMap<String, Array<FormValidator>>().also {
        if (isIpv6) {
            it[LoginFormFields.HOST_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY, FormValidator.IP_ADDRESS_V6)
        } else {
            it[LoginFormFields.HOST_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY, FormValidator.IP_ADDRESS_V4)
        }

        it[LoginFormFields.PORT_FIELD.key] = arrayOf(FormValidator.NUMERIC_POSITIVE, FormValidator.PORT_INTEGER)
        if (isSnmpv3) {
            it[LoginFormFields.USER_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY)
            it[LoginFormFields.PW_FIELD.key] = arrayOf(FormValidator.LONGER_THAN_8)
            it[LoginFormFields.ENC_FIELD.key] = arrayOf(FormValidator.LONGER_THAN_8)
        } else {
            it[LoginFormFields.COMMUNITY_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY)
        }
    }
}

fun getCustomQueryFormElements(): Map<String, Array<FormValidator>> {
    return HashMap<String, Array<FormValidator>>().also {
        it[CustomQueryFormFields.NAME_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY)
        it[CustomQueryFormFields.OID_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY, FormValidator.OID_STRING)
        it[CustomQueryFormFields.IS_SIMPLE_QUERY_FIELD.key] = arrayOf(FormValidator.NOT_EMPTY)
    }
}

fun hasFieldErrors(validationMap: Map<String, List<String>>, validationKey: String): Boolean {
    if (validationKey !in validationMap) {
        return false
    }
    if ((validationMap[validationKey]?.size ?: 0) > 0) {
        return true
    }
    return false
}

/**
 * this class represents the validation structure of the connection "login" form
 */
data class LoginFormValues(val formData: Map<String, String>, val _isIpv6: Boolean, val _isSnmpv3: Boolean) {
    val host: String
        get() = formData.getOrDefault(LoginFormFields.HOST_FIELD.key, "").trim()
    val port: String
        get() = formData.getOrDefault(LoginFormFields.PORT_FIELD.key, "").trim()
    val community: String
        get() = formData.getOrDefault(LoginFormFields.COMMUNITY_FIELD.key, "").trim()
    val user: String
        get() = formData.getOrDefault(LoginFormFields.USER_FIELD.key, "").trim()
    val authPassphrase: String
        get() = formData.getOrDefault(LoginFormFields.PW_FIELD.key, "").trim()
    val encryptionKey: String
        get() = formData.getOrDefault(LoginFormFields.ENC_FIELD.key, "").trim()
    val isIpv6: Boolean
        get() = _isIpv6
    val isSnmpv3: Boolean
        get() = _isSnmpv3
}

/**
 * class to validate form data via #isValid
 * form validation messages are written to input validation map and the general success state is returned
 */
class ValidatedForm(
    val elements: Map<String, Array<FormValidator>>,
) {
    fun isValid(
        context: Context, formData: Map<String, String>, validationMap: SnapshotStateMap<String, List<String>>,
    ): Boolean {
        validationMap.clear()

        elements.forEach { (key, validatorList) ->
            if (formData.containsKey(key)) {
                validatorList.forEach {
                    if (!it.isValid(formData[key])) {
                        if (validationMap.containsKey(key)) {
                            // append
                            if (it.errorMsg is Int) {
                                validationMap[key] =
                                    arrayListOf(context.getString(it.errorMsg), *(validationMap[key]!!.toTypedArray()))
                            } else {
                                validationMap[key] =
                                    arrayListOf(it.errorMsg as String, *(validationMap[key]!!.toTypedArray()))
                            }
                        } else {
                            // add
                            if (it.errorMsg is Int) {
                                validationMap[key] = arrayListOf(context.getString(it.errorMsg))
                            } else {
                                validationMap[key] = arrayListOf(it.errorMsg as String)
                            }
                        }
                    }
                }
            } else {
                Log.w(ValidatedForm::class.java.simpleName, "invalid form key '%s'".format(key))
            }
        }
        if (validationMap.size > 0) {
            return false
        }
        return true
    }
}

@Stable
sealed class FormValidator(
    val key: String, val errorMsg: Any, // Int or String TODO make resource id
    val isValid: (value: String?) -> Boolean,
) {
    object NOT_EMPTY : FormValidator("notEmpty",
                                     R.string.form_validation_msg_not_empty,
                                     fun(value: String?): Boolean { return value != null && value.isNotBlank() })

    object LONGER_THAN_8 : FormValidator("longerThan8",
                                         R.string.form_validation_longer_than_8,
                                         fun(value: String?): Boolean { return value != null && value.isNotBlank() && value.length > 5 })

    object NUMERIC_POSITIVE : FormValidator("numeric", R.string.form_validation_numeric, fun(value: String?): Boolean {
        return try {
            value != null && value.isNotBlank() && Integer.valueOf(value) >= 0
        } catch (ex: NumberFormatException) {
            false
        }
    })

    object IP_ADDRESS_V4 : FormValidator("ip_address_v4", R.string.form_validation_ipv4, fun(value: String?): Boolean {
        if (value == null || value.isBlank()) {
            return false
        }
        return InetAddressValidator.getInstance()
            .isValidInet4Address(value)
    })

    object IP_ADDRESS_V6 : FormValidator("ip_address_v6", R.string.form_validation_ipv6, fun(value: String?): Boolean {
        if (value == null || value.isBlank()) {
            return false
        }
        return InetAddressValidator.getInstance().isValidInet6Address(value)
    })

    object PORT_INTEGER : FormValidator("portInt", R.string.form_validation_network_port, fun(value: String?): Boolean {
        try {
            return value != null && value.isNotBlank() && Integer.valueOf(value) > 0 && Integer.valueOf(value) < 65536
        } catch (ex: NumberFormatException) {
            return false
        }
    })

    object OID_STRING : FormValidator("oidString", R.string.form_validation_oid_string, fun(value: String?): Boolean {
        return value != null && value.isNotBlank() && OidValidator.isOidValid(value)
    })
}

enum class LoginFormFields(val key: String) {
    HOST_FIELD("hostField"), PORT_FIELD("portField"), COMMUNITY_FIELD("communityField"), USER_FIELD(
        "userField"
    ),
    PW_FIELD(
        "pwField"
    ),
    ENC_FIELD("encField"),
}

enum class CustomQueryFormFields(val key: String) {
    NAME_FIELD("nameField"), OID_FIELD("oidField"), IS_SIMPLE_QUERY_FIELD("isSimpleQuery"), IS_SHOWN_IN_DETAILS(
        "isShownInDetails"
    )
}