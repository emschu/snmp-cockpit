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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.emschu.snmp.cockpit.snmp.json.DeviceQrCode
import org.emschu.snmp.cockpit.snmp.json.QrCodeAddressPart
import org.emschu.snmp.cockpit.ui.screens.LoginView
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * TODO use resource strings
 */
@OptIn(ExperimentalComposeUiApi::class)
class ConnectionFormInputTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @Before
    fun setup() {
        // Start the app
        composeTestRule.setContent {
            CockpitTheme {
                LoginView(
                    loginViewModel = loginViewModel, mainViewModel = mainViewModel
                )
            }
        }
    }

    @Test
    fun formWorksWithIpv6AndV4() {
        resetForm()
        loginViewModel.isIpv6.postValue(true)
        checkFormWorks()
        loginViewModel.isIpv6.postValue(false)
        checkFormWorks()
    }

    @Test
    fun versionToggleWorks() {
        checkFormWorks()
    }

    private fun checkFormWorks() {
        // test snmp v3 mode
        loginViewModel.isSnmpv3.postValue(true)
        composeTestRule.onNodeWithText(appContext.getString(R.string.host_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.port_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.community_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(appContext.getString(R.string.password_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.snmpv3_key))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.user_label))
            .assertIsDisplayed()

        // v1/v2c
        loginViewModel.isSnmpv3.postValue(false)
        composeTestRule.onNodeWithText(appContext.getString(R.string.host_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.port_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.community_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appContext.getString(R.string.password_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(appContext.getString(R.string.snmpv3_key))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(appContext.getString(R.string.user_label))
            .assertDoesNotExist()
    }

    // test the qr code population
    @Test
    fun populateFormWorks() {
        resetForm()
        val testUserString = "testcommunity" // this is the user name for snmp v3
        val testEncString = "testEncString"
        val testPwString = "testPwString"
        val testHostName = "127.0.0.5"

        // testing snmpv1 version first
        val deviceQrCode = DeviceQrCode(testUserString, QrCodeAddressPart(testHostName))
        composeTestRule.runOnUiThread {
            loginViewModel.populateForm(deviceQrCode = deviceQrCode)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(appContext.getString(R.string.host_label))
            .assertIsDisplayed()
            .assert(hasText(testHostName))
        composeTestRule.onNodeWithText(appContext.getString(R.string.port_label))
            .assertIsDisplayed()
            .assert(hasText("" + 161))
        composeTestRule.onNodeWithText(appContext.getString(R.string.community_label))
            .assertIsDisplayed()
            .assert(hasText(testUserString))
        composeTestRule.onNodeWithText(appContext.getString(R.string.password_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(appContext.getString(R.string.snmpv3_key))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(appContext.getString(R.string.user_label))
            .assertDoesNotExist()

        // snmpv3
        val testHostName2 = "127.0.0.6:162"
        val deviceQrCode2 = DeviceQrCode(testUserString, QrCodeAddressPart(testHostName2))
        deviceQrCode2.pw = testPwString
        deviceQrCode2.enc = testEncString
        composeTestRule.runOnUiThread {
            loginViewModel.populateForm(deviceQrCode = deviceQrCode2)
        }
        composeTestRule.onNodeWithText(appContext.getString(R.string.host_label))
            .assertIsDisplayed()
            .assert(hasText(testHostName2.split(":")[0])) // strip the port
        composeTestRule.onNodeWithText(appContext.getString(R.string.port_label))
            .assertIsDisplayed()
            .assert(hasText("" + 162))
        composeTestRule.onNodeWithText(appContext.getString(R.string.community_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Password Visibility")
            .performClick()
        composeTestRule.onNodeWithText(appContext.getString(R.string.password_label))
            .assertIsDisplayed()
            .assert(hasText(testPwString))
        composeTestRule.onNodeWithContentDescription("Key Visibility")
            .performClick()
        composeTestRule.onNodeWithText(appContext.getString(R.string.snmpv3_key))
            .assertIsDisplayed()
            .assert(hasText(testEncString))
        composeTestRule.onNodeWithText(appContext.getString(R.string.user_label))
            .assertIsDisplayed()
            .assert(hasText(testUserString))
    }

    @Test
    fun hostInputValidationWorksIpv4() {
        val invalidIPsv4 = listOf("10", "10.10.10.", "127001", "123123", "-1", "text", "example.com", "::1")
        val validIPsv4 = listOf("10.10.10.10", "127.0.0.1")
        testHostIpInputValidation(invalidIPsv4, validIPsv4)
    }

    @Test
    fun hostInputValidationWorksIpv6() {
        val invalidIPsv6 = listOf("10", "10.10.10.", "127001", "123123", "-1", "text", "example.com")
        val validIPsv6 = listOf("::1", "fe80::eeee:12ff:12ff:12ff", "fd00::91cd:91cd:91cd:91cd", "2001:4860:4860::8888")
        testHostIpInputValidation(invalidIPsv6, validIPsv6, true)
    }

    private fun testHostIpInputValidation(
        invalidIPs: List<String>,
        validIPs: List<String>,
        isIpv6: Boolean = false,
    ) {
        resetForm()
        val hostLabel = composeTestRule.onNodeWithText(appContext.getString(R.string.host_label))

        loginViewModel.isIpv6.postValue(isIpv6)
        composeTestRule.onNodeWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(
            appContext.getString(if (isIpv6) R.string.form_validation_ipv6 else R.string.form_validation_ipv4)
        )
            .assertDoesNotExist()

        hostLabel.assertIsDisplayed()

        // test the empty string
        loginViewModel.host.postValue("")
        composeTestRule.runOnUiThread {
            // this triggers form validation
            loginViewModel.login(appContext, mainViewModel)
        }

        fun expectNotEmptyAndIpValidationErrCount(notEmptyCount: Int = 0, isValidIp: Boolean = true) {
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_msg_not_empty))
                .assertCountEquals(notEmptyCount)
            if (loginViewModel.isIpv6.value!!) {
                composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_ipv6))
                    .assertCountEquals(if (isValidIp) 0 else 1)
            } else {
                composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_ipv4))
                    .assertCountEquals(if (isValidIp) 0 else 1)
            }
        }

        expectNotEmptyAndIpValidationErrCount(2, false)

        // test invalid ips
        invalidIPs.forEach { invalidIp ->
            resetForm()
            setIpAndTriggerForm(invalidIp)
            expectNotEmptyAndIpValidationErrCount(0, false)

            // v3
            resetForm()
            setIpAndTriggerForm(invalidIp, true)
            expectNotEmptyAndIpValidationErrCount(0, false)
        }
        validIPs.forEach { validIp ->
            resetForm()
            setIpAndTriggerForm(validIp)
            expectNotEmptyAndIpValidationErrCount(0, true)
            // v3
            resetForm()
            setIpAndTriggerForm(validIp, true)
            expectNotEmptyAndIpValidationErrCount(0, true)
        }
    }

    @Test
    fun portInputValidationWorks() {
        resetForm()
        loginViewModel.isIpv6.postValue(false)
        loginViewModel.host.postValue("127.0.0.1")
        loginViewModel.community.postValue("testCommunity")
        loginViewModel.port.postValue("")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(1)

        val invalidPortsDueToNumericValidation = arrayListOf<String>("-1", "-100", "-161")
        val invalidPortsDueToPortValidation = arrayListOf<String>("0", "65599", "65536")
        val validPorts = arrayListOf<String>(
            "161", "162", "30000", "65535"
        )
        invalidPortsDueToNumericValidation.forEach { invalidPort ->
            loginViewModel.port.postValue(invalidPort)
            composeTestRule.runOnUiThread {
                loginViewModel.login(appContext, mainViewModel)
            }
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_numeric))
                .assertCountEquals(1)
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_network_port))
                .assertCountEquals(1)
        }
        invalidPortsDueToPortValidation.forEach { invalidPort ->
            loginViewModel.port.postValue(invalidPort)
            composeTestRule.runOnUiThread {
                loginViewModel.login(appContext, mainViewModel)
            }
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_numeric))
                .assertCountEquals(0)
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_network_port))
                .assertCountEquals(1)
        }
        validPorts.forEach { invalidPort ->
            loginViewModel.port.postValue(invalidPort)
            composeTestRule.runOnUiThread {
                loginViewModel.login(appContext, mainViewModel)
            }
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_numeric))
                .assertCountEquals(0)
            composeTestRule.onAllNodesWithText(appContext.getString(R.string.form_validation_network_port))
                .assertCountEquals(0)
        }
    }

    @Test
    fun communityInputValidationWorks() {
        resetForm()
        loginViewModel.isIpv6.postValue(false)
        loginViewModel.isSnmpv3.postValue(false)
        loginViewModel.host.postValue("127.0.0.1")
        loginViewModel.community.postValue("")
        loginViewModel.port.postValue("161")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(1)

        loginViewModel.community.postValue("test")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
    }

    @Test
    fun userInputValidationWorks() {
        resetForm()
        loginViewModel.isIpv6.postValue(false)
        loginViewModel.isSnmpv3.postValue(true)
        loginViewModel.host.postValue("127.0.0.1")
        loginViewModel.community.postValue("")
        loginViewModel.port.postValue("161")
        loginViewModel.key.postValue("testKey")
        loginViewModel.password.postValue("testPassword")
        loginViewModel.user.postValue("")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(1)

        loginViewModel.user.postValue("test")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
    }

    @Test
    fun passwordInputValidationWorks() {
        resetForm()
        loginViewModel.isIpv6.postValue(false)
        loginViewModel.isSnmpv3.postValue(true)
        loginViewModel.host.postValue("127.0.0.1")
        loginViewModel.community.postValue("")
        loginViewModel.port.postValue("161")
        loginViewModel.key.postValue("testKey")
        loginViewModel.user.postValue("testUser")
        loginViewModel.password.postValue("")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_longer_than_8)
        )
            .assertCountEquals(1)

        loginViewModel.password.postValue("test")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_longer_than_8)
        )
            .assertCountEquals(1)

        loginViewModel.password.postValue("test_long_enough")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_longer_than_8)
        )
            .assertCountEquals(0)
    }

    @Test
    fun keyInputValidationWorks() {
        resetForm()
        loginViewModel.isIpv6.postValue(false)
        loginViewModel.isSnmpv3.postValue(true)
        loginViewModel.host.postValue("127.0.0.1")
        loginViewModel.community.postValue("")
        loginViewModel.port.postValue("161")
        loginViewModel.key.postValue("")
        loginViewModel.user.postValue("testUser")
        loginViewModel.password.postValue("testPassword")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_longer_than_8)
        )
            .assertCountEquals(1)

        loginViewModel.key.postValue("test")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_longer_than_8)
        )
            .assertCountEquals(1)

        loginViewModel.key.postValue("test_long_enough")
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_msg_not_empty)
        )
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.form_validation_longer_than_8)
        )
            .assertCountEquals(0)
    }


    @OptIn(ExperimentalComposeUiApi::class)
    private fun setIpAndTriggerForm(validIp: String, isSnmpv3: Boolean = false) {
        loginViewModel.isSnmpv3.postValue(isSnmpv3)
        loginViewModel.port.postValue("" + 161)
        loginViewModel.community.postValue("testCommunity")
        loginViewModel.user.postValue("testUser")
        loginViewModel.key.postValue("testKey")
        loginViewModel.host.postValue(validIp)
        composeTestRule.runOnUiThread {
            loginViewModel.login(appContext, mainViewModel)
        }
    }

    fun resetForm() {
        loginViewModel.host.postValue("")
        loginViewModel.port.postValue("161")
        loginViewModel.community.postValue("")
        loginViewModel.key.postValue("")
        loginViewModel.password.postValue("")
        loginViewModel.user.postValue("")
    }
}
