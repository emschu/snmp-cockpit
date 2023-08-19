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

package org.emschu.snmp.cockpit.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes

fun makeNotification(context: Context, title: String, message: String) {
    return makeToastNotification(context, title, message)
}

fun makeNotification(context: Context, title: String, @StringRes message: Int) {
    makeNotification(context, title, context.getString(message))
}

fun makeNotification(context: Context, @StringRes title: Int, @StringRes message: Int) {
    makeNotification(context, context.getString(title), context.getString(message))
}

fun makeNotification(context: Context, @StringRes title: Int, message: String) {
    makeNotification(context, context.getString(title), message)
}

private fun makeToastNotification(context: Context, title: String, message: String) {
    Handler(Looper.getMainLooper()).post {
        val notificationContent = if (title.isNotEmpty()) {
            "%s: %s".format(title, message)
        } else {
            message
        }
        Toast.makeText(context, notificationContent, Toast.LENGTH_SHORT)
            .show()
    }
}