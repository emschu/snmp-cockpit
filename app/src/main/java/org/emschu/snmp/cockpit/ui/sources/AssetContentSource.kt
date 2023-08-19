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

package org.emschu.snmp.cockpit.ui.sources

import android.content.Context

enum class MarkdownAssetFile(val fileName: String) {
    LICENSE("LICENSE"),
    PRIVACY_POLICY("PRIVACY_POLICY.md"),
    DOCS_DEVICE_QR_CODE("QR_code_guide.md"),
    DOCS_MIB_CATALOG("MIB_catalog_guide.md"),
}

object AssetContentSource {

    private var licenseFile: String = ""
    private var privPolicy: String = ""
    private var device_qr_code: String = ""
    private var mib_catalog_docs: String = ""

    fun initialLoading(context: Context) {
        val openFile: (MarkdownAssetFile) -> String = { asset ->
            context.assets.open(asset.fileName)
                .readBytes().decodeToString()
        }
        licenseFile = openFile(MarkdownAssetFile.LICENSE)
        privPolicy = openFile(MarkdownAssetFile.PRIVACY_POLICY)
        device_qr_code = openFile(MarkdownAssetFile.DOCS_DEVICE_QR_CODE)
        mib_catalog_docs = openFile(MarkdownAssetFile.DOCS_MIB_CATALOG)
    }

    fun getFileForMarkdownAssetFile(assetType: MarkdownAssetFile): String {
        return when (assetType) {
            MarkdownAssetFile.LICENSE -> this.licenseFile
            MarkdownAssetFile.PRIVACY_POLICY -> this.privPolicy
            MarkdownAssetFile.DOCS_DEVICE_QR_CODE -> this.device_qr_code
            MarkdownAssetFile.DOCS_MIB_CATALOG -> this.mib_catalog_docs
        }
    }
}