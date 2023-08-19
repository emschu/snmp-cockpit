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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Transparent = Color(0x00000000)

// original!
//val PrimaryViolet = Color(0xFF5a0e42)

val PrimaryLightColor = Color(0xFF00abD4)
val PrimaryDarkColor = Color(0xff002984)
val PrimaryTextColorDark = Color(0xFFFFFFFF)
val PrimaryTextColorLight = Color(0xFF000000)
val PrimaryWarnColor = Color(0xFFFF9800)

val SecondaryLightColor = Color(0xFF673AB7)
val SecondaryDarkColor = Color(0xFF007c91)
val SecondaryTextColor = Color(0xFFffffff)
val SignalGreenYellow = Color(0xfffdd835)

val AccentLight = Color(0xffefffff)
val AccentDark = Color(0xff007c91)

@Composable
fun accentBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) {
        AccentDark
    } else {
        AccentLight
    }
}