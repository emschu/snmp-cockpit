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

package org.emschu.snmp.cockpit.tasks

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File


/**
 * this task deletes a file in external-files dir which was generated to share.
 * This task deletes this file (after a certain time).
 *
 * Param: FILE_PATH
 */
class DeleteTmpFileTask(
    private val context: Context,
    private val workerParams: WorkerParameters,
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val string = workerParams.inputData.getString(FILE_PATH) ?: ""
        if (string.isNotBlank()) {
            val fileToDelete = File("%s/%s".format(context.getExternalFilesDir("external_files"), string))
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d("TmpFileDelete", "Tmpfile deleted!")
                } else {
                    Log.w("TmpFileDelete", "Tmpfile NOT deleted!")
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val FILE_PATH = "FILE_PATH_ARG"
    }
}