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

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.emschu.snmp.cockpit.BuildConfig
import org.emschu.snmp.cockpit.R
import org.emschu.snmp.cockpit.tasks.DeleteTmpFileTask
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * the tmp file with sensitive snmp connection credentials is deleted later by a work manager task
 */
fun createTmpImageAndShare(context: Context, byteArray: ByteArray, deleteAfterXSeconds: Int = 60) {
    val intent = Intent(Intent.ACTION_SEND)
    val openFileOutput = File(context.getExternalFilesDir("external_files"), "qr_code_snmp_share.png")
    openFileOutput.writeBytes(byteArray)

    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.type = "image/png"

    val uriForFile = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", openFileOutput)

    val wm = WorkManager.getInstance(context)
    val toString = uriForFile.lastPathSegment
    val jobData = Data.Builder().putString(DeleteTmpFileTask.FILE_PATH, toString).build()

    val build = OneTimeWorkRequest.Builder(DeleteTmpFileTask::class.java)
        .setInputData(jobData)
        .setInitialDelay(deleteAfterXSeconds.toLong(), TimeUnit.SECONDS)
        .build()
    wm.enqueueUniqueWork(
        "delete_tmp_file_${Calendar.getInstance().timeInMillis}",
        ExistingWorkPolicy.APPEND,
        build
    )

    intent.putExtra(Intent.EXTRA_STREAM, uriForFile)
    val createChooser = Intent.createChooser(intent, context.getString(R.string.share_qr_image_dialog_title))
    context.startActivity(createChooser)
}