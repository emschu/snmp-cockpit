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

package org.emschu.snmp.cockpit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = PrimaryDarkColor,
    primaryVariant = PrimaryDarkColor,
    secondary = SecondaryDarkColor,
    secondaryVariant = SecondaryDarkColor,
    onPrimary = PrimaryTextColorDark,
    onSecondary = SecondaryTextColor
)

private val LightColorPalette = lightColors(
    primary = PrimaryLightColor,
    primaryVariant = PrimaryLightColor,
    secondary = SecondaryLightColor,
    secondaryVariant = SecondaryLightColor,
    onPrimary = PrimaryTextColorLight,
    onSecondary = SecondaryTextColor
)

@Composable
fun CockpitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit,
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors, typography = Typography, shapes = Shapes, content = content
    )
}