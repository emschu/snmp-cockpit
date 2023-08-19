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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mukesh.MarkDown
import org.emschu.snmp.cockpit.ui.sources.AssetContentSource
import org.emschu.snmp.cockpit.ui.sources.MarkdownAssetFile

@Composable
fun MarkdownFromAsset(
    assetFile: MarkdownAssetFile, modifier: Modifier = Modifier,
) {
    val fileContent = remember { AssetContentSource.getFileForMarkdownAssetFile(assetFile) }
    Box(modifier = modifier.padding(2.dp)) {
        MarkDown(
            modifier = modifier,
            text = fileContent,
        )
    }
}