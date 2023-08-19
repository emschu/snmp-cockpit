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

package org.emschu.snmp.cockpit.util

import android.os.Parcel
import android.os.Parcelable


internal inline fun <reified T : Parcelable> ByteArray.deserializeParcelable(): T {
    val parcel = Parcel.obtain().apply {
        unmarshall(this@deserializeParcelable, 0, size)
        setDataPosition(0)
    }

    return parcelableCreator<T>().createFromParcel(parcel).also {
        parcel.recycle()
    }
}

internal inline fun <reified T : Parcelable> parcelableCreator(): Parcelable.Creator<T> {
    val creator = T::class.java.getField("CREATOR").get(null)
    @Suppress("UNCHECKED_CAST") return creator as Parcelable.Creator<T>
}