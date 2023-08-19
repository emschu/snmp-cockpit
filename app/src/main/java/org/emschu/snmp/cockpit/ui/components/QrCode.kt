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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.emschu.snmp.cockpit.snmp.DeviceConfiguration
import java.io.ByteArrayOutputStream


@Composable
fun ClickableQrCode(
    deviceConfiguration: DeviceConfiguration,
    isWithPwPhrase: Boolean,
    isWithEncPhrase: Boolean,
    onClick: (ByteArray) -> Unit,
) {
    val qrCodeContent = generateQrCodeContentForDevice(deviceConfiguration, isWithPwPhrase, isWithEncPhrase)
    val writer = QRCodeWriter()
    val barcodeEncoder = BarcodeEncoder()
    val bitMatrix = writer.encode(qrCodeContent, BarcodeFormat.QR_CODE, 500, 500)
    val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
    val bitmapPngByteArray = run {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 60, bos)
        bos.use {
            it.toByteArray()
        }
    }

    Image(alignment = Alignment.Center,
          bitmap = bitmap.asImageBitmap(),
          contentDescription = "QR Code",
          modifier = Modifier.clickable { onClick(bitmapPngByteArray) })
}

fun generateQrCodeContentForDevice(
    deviceConfiguration: DeviceConfiguration,
    isWithPwPhrase: Boolean = false,
    isWithEncPhrase: Boolean = false,
): String {
    val userName = deviceConfiguration.username.trim()
    var pwPhrase = ""
    var encPhrase = ""
    var ipv4Addr = ""
    var ipv6Addr = ""

    if (deviceConfiguration.isIpv6) {
        ipv6Addr = deviceConfiguration.targetIp
    } else {
        ipv4Addr = deviceConfiguration.targetIp
    }

    if (deviceConfiguration.isV3) {
        pwPhrase = deviceConfiguration.privacyPassphrase
        encPhrase = deviceConfiguration.authPassphrase
    }
    val pwString = if (isWithPwPhrase) pwPhrase else ""
    val encString = if (isWithEncPhrase) encPhrase else ""

    return """{"user": "$userName", "pw": "$pwString", "enc": "$encString", "naddr": {"IPv4": "$ipv4Addr", "IPv6": "$ipv6Addr"}}"""
}
