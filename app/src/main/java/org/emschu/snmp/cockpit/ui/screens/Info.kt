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

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.launch
import org.emschu.snmp.cockpit.BuildConfig
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.ui.components.AppLogoWithText
import org.emschu.snmp.cockpit.ui.components.MarkdownFromAsset
import org.emschu.snmp.cockpit.ui.components.Screen
import org.emschu.snmp.cockpit.ui.sources.MarkdownAssetFile
import org.emschu.snmp.cockpit.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AppInformationView(navController: NavHostController, viewModel: MainViewModel) {
    InfoScreen(navController, viewModel)
}

/**
 * these objects represent the info sections using an enum InfoScreen as id
 */
@Stable
sealed class InfoSectionScreen(
    val index: InfoScreen,
    @StringRes val titleResId: Int,
) {
    object License : InfoSectionScreen(InfoScreen.LICENSE, R.string.info_license)
    object DeviceQrCodeDocs : InfoSectionScreen(InfoScreen.DEVICE_QR_DOCS, R.string.info_device_qr_code_schema)
    object MibCatalogDocs : InfoSectionScreen(InfoScreen.MIB_CATALOG_DOCS, R.string.info_mib_catalog_docs)

    object GitHubRepository : InfoSectionScreen(InfoScreen.GITHUB_REPOSITORY, R.string.info_github_weblink_label)

    object VersionInformation :
        InfoSectionScreen(InfoScreen.VERSION_DEPENDENCY_INFORMATION, R.string.info_version_and_libraries_info_label)

    object DataUsagePolicy : InfoSectionScreen(InfoScreen.DATA_USAGE_POLICY, R.string.info_privacy_policy)

    object ShowAppIntro : InfoSectionScreen(InfoScreen.SHOW_APP_INTRO, R.string.info_app_tour)
}


enum class InfoScreen {
    SHOW_APP_INTRO,
    LICENSE,
    DEVICE_QR_DOCS,
    MIB_CATALOG_DOCS,
    GITHUB_REPOSITORY,
    VERSION_DEPENDENCY_INFORMATION,
    DATA_USAGE_POLICY,
}

val infoScreens = listOf(
    InfoSectionScreen.License,
    InfoSectionScreen.DeviceQrCodeDocs,
    InfoSectionScreen.MibCatalogDocs,
    InfoSectionScreen.GitHubRepository,
    InfoSectionScreen.VersionInformation,
    InfoSectionScreen.DataUsagePolicy,
    InfoSectionScreen.ShowAppIntro,
)

@Composable
fun InfoScreen(navController: NavHostController, mainViewModel: MainViewModel) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // TODO show app's icon/logo
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val openGitHubIntent = remember {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/emschu/snmp-cockpit"))
        }
        val currentYear = remember {
            mutableStateOf(
                Calendar.getInstance()
                    .get(Calendar.YEAR)
            )
        }
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            AppLogoWithText()
        }
        Row(Modifier.padding(top = 12.dp)) {
            Column(
                Modifier.padding(start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text(
                        "SNMP Cockpit\n2018-${currentYear.value} by emschu",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "app info text" }
                            .wrapContentWidth())
                }
                Row(Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.app_license_information),
                         textAlign = TextAlign.Center,
                         fontSize = 14.sp,
                         modifier = Modifier
                             .semantics { contentDescription = "license info text" }
                             .fillMaxWidth()
                             .wrapContentWidth())
                }
                Row {
                    Text(
                        String.format(
                            "App build time: %s", SimpleDateFormat(
                                "dd.MM.yyyy, HH:mm:ss", Locale.ENGLISH
                            ).format(Date(BuildConfig.BUILD_TIMESTAMP))
                        ),
                        fontSize = 12.sp,
                    )
                }
            }
        }
        infoScreens.forEach {
            ShowInfoSectionRow(text = stringResource(id = it.titleResId)) {
                if (it.index == InfoScreen.GITHUB_REPOSITORY) {
                    context.startActivity(openGitHubIntent)
                } else {
                    coroutineScope.launch { mainViewModel.navigateToInfoSection(navController, it.index) }
                }
            }
        }

    }
}

@Composable
fun InfoSectionDetailView(sectionId: InfoScreen, mainViewModel: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current

    // license screen is default
    mainViewModel.updateScreenTitle(stringResource((infoScreens.firstOrNull { it.index == sectionId }
        ?: InfoSectionScreen.License).titleResId))

    when (sectionId) {
        InfoScreen.LICENSE -> {
            MarkdownFromAsset(MarkdownAssetFile.LICENSE)
        }

        InfoScreen.DEVICE_QR_DOCS -> {
            MarkdownFromAsset(MarkdownAssetFile.DOCS_DEVICE_QR_CODE)
        }

        InfoScreen.MIB_CATALOG_DOCS -> {
            MarkdownFromAsset(MarkdownAssetFile.DOCS_MIB_CATALOG)
        }

        InfoScreen.VERSION_DEPENDENCY_INFORMATION -> {
            LibrariesContainer(Modifier.fillMaxSize(), librariesBlock = {
                val builder = Libs.Builder()
                    .withContext(it)
                    .build()
                builder
            })
        }
        // not handled here (because they are "actions")
        // InfoScreen.MAIL_FEEDBACK
        // InfoScreen.GITHUB_REPOSITORY
        InfoScreen.DATA_USAGE_POLICY -> {
            MarkdownFromAsset(MarkdownAssetFile.PRIVACY_POLICY)
        }

        InfoScreen.SHOW_APP_INTRO -> {
            LaunchedEffect(key1 = "", block = {
                val intent = Intent(context.applicationContext, CockpitAppIntro::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.applicationContext.startActivity(intent)
                mainViewModel.navigateTo(navController, Screen.HOME)
            })
        }
        // the following "screens" aka buttons are not displayed
        InfoScreen.GITHUB_REPOSITORY -> return
    }
}

@Composable
private fun ShowInfoSectionRow(text: String, buttonClick: () -> (Unit)) {
    Row(
        Modifier
            .padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 4.dp)
            .fillMaxWidth()
    ) {
        Button(
            onClick = { buttonClick() },
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = contentColorFor(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text, fontSize = MaterialTheme.typography.body1.fontSize)
        }
    }
}

@Preview
@Composable
private fun InfoScreenPreview() {
    InfoSectionDetailView(sectionId = InfoScreen.LICENSE, MainViewModel(), rememberNavController())
}