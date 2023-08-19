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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import org.emschu.snmp.cockpit.ui.theme.CockpitTheme

@Composable
fun ToggleChip(
    onClick: (Boolean) -> Unit,
    onText: String,
    toggleState: Boolean,
    offText: String = onText,
    icon: ImageVector = Icons.Filled.PushPin,
    fontSize: TextUnit = MaterialTheme.typography.h5.fontSize,
) {
    Surface(
        modifier = Modifier
            .padding(all = 4.dp)
            .toggleable(toggleState,
                        onValueChange = {
                            onClick(it)
                        }),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp, color = MaterialTheme.colors.secondary
        ),
        contentColor = MaterialTheme.colors.onPrimary,
        content = {
            Column(modifier = Modifier.background(MaterialTheme.colors.secondary)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Pin",
                        tint = MaterialTheme.colors.onSecondary,
                        modifier = Modifier.padding(4.dp),
                    )

                    Text(
                        if (onText != offText && !toggleState) {
                            offText
                        } else {
                            onText
                        },
                        modifier = Modifier.padding(4.dp),
                        fontSize = fontSize,
                        color = MaterialTheme.colors.onSecondary,
                    )
                }
            }
        })
}

@Preview
@Composable
private fun ToggleChipPreview() {
    CockpitTheme(darkTheme = true) {
        ToggleChip(onClick = {}, onText = "Text", toggleState = true, offText = "offText")
    }
}