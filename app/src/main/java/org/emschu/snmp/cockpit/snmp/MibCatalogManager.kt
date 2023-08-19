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

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.emschu.snmp.cockpit.CockpitPreferenceManager
import org.emschu.snmp.cockpit.snmp.json.MibCatalog
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * this class manages the user's imported mib catalogs and stores them as serialized json in a string preference
 */
class MibCatalogManager(private val sharedPreferences: SharedPreferences) {
    private val objectMapper: ObjectMapper = ObjectMapper()
    var mibCatalog: MutableList<MibCatalog>
        private set

    init {
        val newList = mutableListOf<MibCatalog>()
        newList.addAll(retrieveMibCatalog())
        mibCatalog = newList
    }

    fun storeCatalog() {
        storeMibCatalog(mibCatalog)
    }

    private fun retrieveMibCatalog(): List<MibCatalog> {
        val currentCatalogString = sharedPreferences.getString(CockpitPreferenceManager.KEY_MIB_CATALOG_CONTENT, null)
        if (currentCatalogString == null) {
            Log.d(TAG, "creating default MIB catalog storage backend")
            return listOf()
        }
        Log.d(TAG, currentCatalogString)
        try {
            return objectMapper.readValue(currentCatalogString, object : TypeReference<List<MibCatalog>>() {})
                ?: return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException during read of stored JSON MIB catalog: " + e.message)
        } catch (e: IllegalArgumentException) {
            // most likely a (permission) problem accessing the storage
            Log.e(TAG, "IllegalArgumentException during read of stored JSON MIB catalog: " + e.message)
        }
        return emptyList()
    }

    private fun createNewDefaultCatalog() {
        val catalogList: MutableList<MibCatalog> = ArrayList()
        catalogList.add(MibCatalog(DEFAULT_CATALOG))
        storeMibCatalog(catalogList)

        val newList = mutableListOf<MibCatalog>()
        newList.addAll(retrieveMibCatalog())
        mibCatalog = newList
    }

    private fun storeMibCatalog(catalogList: List<MibCatalog>) {
        try {
            val om = ObjectMapper()
            val jsonString = om.writeValueAsString(catalogList.toTypedArray())
            Log.d(TAG, "mib catalog json string: $jsonString")
            val edit = sharedPreferences.edit()
            edit.putString(CockpitPreferenceManager.KEY_MIB_CATALOG_CONTENT, jsonString)
            edit.apply()
        } catch (e: JsonProcessingException) {
            Log.e(TAG, "error creating default mib catalog storage: " + e.message)
        }
    }

    fun activateCatalog(archiveName: String?) {
        Log.i(TAG, String.format("activated MIB catalog with name '%s'", archiveName))
        val edit = sharedPreferences.edit()
        edit.putString(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION, archiveName)
        edit.apply()
    }

    fun isDuplicate(newCatalogName: String): Boolean {
        for (mc in mibCatalog) {
            if (mc.catalogName == newCatalogName) {
                return true
            }
        }
        return false
    }

    fun resetToDefault(internalFileDir: File?) {
        for (mc in mibCatalog) {
            val oidCatalogFile =
                File(internalFileDir, mc.catalogName + "_" + MibCatalogArchiveManager.OID_CATALOG_JSON_FILE_NAME)
            val oidTreeFile =
                File(internalFileDir, mc.catalogName + "_" + MibCatalogArchiveManager.OID_TREE_JSON_FILE_NAME)
            if (oidCatalogFile.exists()) {
                if (oidCatalogFile.delete()) {
                    Log.d(TAG, String.format("file '%s' is deleted in internal storage", oidCatalogFile.name))
                } else {
                    Log.w(TAG, String.format("file '%s' could not be deleted in internal storage", oidCatalogFile.name))
                }
            } else {
                Log.w(TAG, String.format("Mib catalog file '%s' does not exist", oidCatalogFile.name))
            }
            if (oidTreeFile.exists()) {
                if (oidTreeFile.delete()) {
                    Log.d(TAG, String.format("file '%s' is deleted in internal storage", oidTreeFile.name))
                } else {
                    Log.w(TAG, String.format("file '%s' could not be deleted in internal storage", oidTreeFile.name))
                }
            } else {
                Log.w(TAG, String.format("Mib tree file '%s' does not exist", oidTreeFile.name))
            }
        }
        // activate default catalog
        activateCatalog(DEFAULT_CATALOG)
        createNewDefaultCatalog()
    }

    @Throws(IOException::class)
    fun getCatalogFileInputStream(context: Context): InputStream {
        val currentMibKey =
            sharedPreferences.getString(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION, null)
        return if (currentMibKey == null || currentMibKey.isEmpty() || currentMibKey == DEFAULT_CATALOG) {
            context.assets.open(MibCatalogArchiveManager.OID_CATALOG_JSON_FILE_NAME)
        } else FileInputStream(
            File(
                context.filesDir, currentMibKey + "_" + MibCatalogArchiveManager.OID_CATALOG_JSON_FILE_NAME
            )
        )
    }

    @Throws(IOException::class)
    fun getTreeFileInputStream(context: Context): InputStream {
        val currentMibKey =
            sharedPreferences.getString(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION, null)
        return if (currentMibKey == null || currentMibKey.isEmpty() || currentMibKey == DEFAULT_CATALOG) {
            context.assets.open(MibCatalogArchiveManager.OID_TREE_JSON_FILE_NAME)
        } else FileInputStream(
            File(
                context.filesDir, currentMibKey + "_" + MibCatalogArchiveManager.OID_TREE_JSON_FILE_NAME
            )
        )
    }

    companion object {
        private val TAG = MibCatalogManager::class.java.name

        private const val DEFAULT_CATALOG = "default_catalog"
    }
}