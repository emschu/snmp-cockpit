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
package org.emschu.snmp.cockpit.snmp

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.emschu.snmp.cockpit.snmp.json.JsonCatalogItem
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * this class manages handling of "uploaded" .zip archives for a new internal mib catalog
 */
class MibCatalogArchiveManager(private val context: Context, private val locationToZip: Uri) {
    val archiveName: String
    val isArchiveValid: Boolean
        get() {
            var hasCatalogFile = false
            var hasTreeFile = false
            var fileName: String
            try {
                context.contentResolver.openInputStream(locationToZip)
                    .use { zipStream ->
                        ZipInputStream(BufferedInputStream(zipStream)).use { zis ->
                            var nextEntry: ZipEntry? = zis.nextEntry
                            while (nextEntry != null) {
                                fileName = nextEntry.name
                                if (fileName == OID_CATALOG_JSON_FILE_NAME) {
                                    hasCatalogFile = true
                                } else if (fileName == OID_TREE_JSON_FILE_NAME) {
                                    hasTreeFile = true
                                } else {
                                    Log.e(TAG, String.format("invalid file name '%s' in import zip file", fileName))
                                    return false
                                }
                                zis.closeEntry()
                                nextEntry = zis.nextEntry
                            }
                        }
                    }
            } catch (e: IOException) {
                Log.e(TAG, String.format("IoException: '%s'", e.message))
                return false
            }
            return hasCatalogFile && hasTreeFile
        }

    /**
     * @return success state
     */
    fun unpackZip(): Boolean {
        var fileName: String
        try {
            context.contentResolver.openInputStream(locationToZip)
                .use { zipStream ->
                    ZipInputStream(BufferedInputStream(zipStream)).use { zis ->
                        var nextEntry: ZipEntry? = zis.nextEntry
                        while (nextEntry != null) {
                            fileName = nextEntry.name
                            if (fileName != OID_CATALOG_JSON_FILE_NAME && fileName != OID_TREE_JSON_FILE_NAME) {
                                Log.w(TAG, String.format("unexpected file '%s' in import mib archive", fileName))
                                continue
                            }

                            // write each single file
                            writeFileToInternalStorage(zis, fileName, archiveName)
                            zis.closeEntry()
                            nextEntry = zis.nextEntry
                        }
                    }
                }
        } catch (e: IOException) {
            Log.e(TAG, "IoException during zip unpacking:" + e.message)
            return false
        }
        return true
    }

    @Throws(IOException::class)
    private fun writeFileToInternalStorage(zis: ZipInputStream, fileName: String, catalogName: String) {
        var count: Int
        val buffer = ByteArray(1024)
        val internalFileName = catalogName + "_" + fileName
        Log.d(TAG, "internal file name: $internalFileName")
        val internalFile = File(context.filesDir, internalFileName)
        try {
            FileOutputStream(internalFile).use { fout ->
                while (zis.read(buffer)
                        .also { count = it } != -1
                ) {
                    fout.write(buffer, 0, count)
                }
                FileInputStream(internalFile).use { fis ->
                    val om = ObjectMapper()
                    if (internalFileName.contains(OID_CATALOG_JSON_FILE_NAME)) {
                        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        om.readValue(
                            fis,
                            object :
                                TypeReference<Map<String, JsonCatalogItem>>() {})
                    } else {
                        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                        val `is`: Reader = BufferedReader(FileReader(internalFile))
                        om.readTree(`is`)
                        `is`.close()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "io exception during processing .zip file")
            throw IOException(e)
        }
    }

    @SuppressLint("Range")
    private fun getArchiveName(locationToZip: Uri): String {
        var detectedArchiveName = ""
        if (locationToZip.scheme != null && locationToZip.scheme == "content") {
            context.contentResolver.query(locationToZip, null, null, null, null)
                .use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        detectedArchiveName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
        } else {
            val lastPathSegment = locationToZip.lastPathSegment
            if (lastPathSegment != null) {
                detectedArchiveName = File(lastPathSegment).name
            } else {
                Log.w(TAG, "No last path segment in Uri found")
            }
        }
        Log.i(TAG, String.format("import archive name '%s'", detectedArchiveName))
        return detectedArchiveName.replace(".zip", "")
    }

    companion object {
        private val TAG = MibCatalogArchiveManager::class.java.name
        const val OID_CATALOG_JSON_FILE_NAME = "oid_catalog.json"
        const val OID_TREE_JSON_FILE_NAME = "oid_tree.json"
    }

    init {
        archiveName = getArchiveName(locationToZip)
    }
}