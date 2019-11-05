/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2018-2019
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit.snmp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * this class manages the user's imported mib catalogs and stores them as serialized json in a string preference
 */
public class MibCatalogManager {

    public static final String TAG = MibCatalogManager.class.getName();
    private final SharedPreferences sharedPreferences;
    private static final String MIB_CATALOG_KEY = "min_catalogs_available";
    private static final String DEFAULT_CATALOG = "default_catalog";
    private final ObjectMapper objectMapper;
    private List<MibCatalog> mibCatalog;

    public MibCatalogManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        objectMapper = new ObjectMapper();
        mibCatalog = retrieveMibCatalog();
    }

    public List<MibCatalog> getMibCatalog() {
        return mibCatalog;
    }

    public void storeCatalog() {
        storeMibCatalog(mibCatalog);
    }

    private List<MibCatalog> retrieveMibCatalog() {
        String currentCatalogString = sharedPreferences.getString(MIB_CATALOG_KEY, null);
        if (currentCatalogString == null) {
            Log.d(TAG, "creating default MIB catalog storage backend");
            createNewDefaultCatalog();
        }
        String catalogJson = sharedPreferences.getString(MIB_CATALOG_KEY, null);
        Log.d(TAG, catalogJson);
        try {
            return objectMapper.readValue(catalogJson, new TypeReference<List<MibCatalog>>() {
            });
        } catch (IOException e) {
            Log.e(TAG, "IOException during read of stored JSON MIB catalog: " + e.getMessage());
        }
        createNewDefaultCatalog();
        return retrieveMibCatalog();
    }

    private void createNewDefaultCatalog() {
        List<MibCatalog> catalogList = new ArrayList<>();
        catalogList.add(new MibCatalog(DEFAULT_CATALOG));
        storeMibCatalog(catalogList);
        mibCatalog = retrieveMibCatalog();
    }

    private void storeMibCatalog(@NonNull List<MibCatalog> catalogList) {
        try {
            ObjectMapper om = new ObjectMapper();
            String jsonString = om.writeValueAsString(catalogList.toArray());
            Log.d(TAG, "mib catalog json string: " + jsonString);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString(MIB_CATALOG_KEY, jsonString);
            edit.apply();
        } catch (JsonProcessingException e) {
            Log.e(TAG, "error creating default mib catalog storage: " + e.getMessage());
        }
    }

    public void activateCatalog(String archiveName) {
        Log.i(TAG, String.format("activated MIB catalog with name '%s'", archiveName));
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION, archiveName);
        edit.apply();
    }

    public boolean isDuplicate(String newCatalogName) {
        for (MibCatalog mc : mibCatalog) {
            if (mc.getCatalogName().equals(newCatalogName)) {
                return true;
            }
        }
        return false;
    }

    public void resetToDefault(File internalFileDir) {
        for (MibCatalog mc : mibCatalog) {
            File oidCatalogFile = new File(internalFileDir, mc.getCatalogName() + "_" + MibCatalogArchiveManager.OID_CATALOG_JSON_FILE_NAME);
            File oidTreeFile = new File(internalFileDir, mc.getCatalogName() + "_" + MibCatalogArchiveManager.OID_TREE_JSON_FILE_NAME);
            if (oidCatalogFile.exists()) {
                if (oidCatalogFile.delete()) {
                    Log.d(TAG, String.format("file '%s' is deleted in internal storage", oidCatalogFile.getName()));
                } else {
                    Log.w(TAG, String.format("file '%s' could not be deleted in internal storage", oidCatalogFile.getName()));
                }
            } else {
                Log.w(TAG, String.format("Mib catalog file '%s' does not exist", oidCatalogFile.getName()));
            }
            if (oidTreeFile.exists()) {
                if (oidTreeFile.delete()) {
                    Log.d(TAG, String.format("file '%s' is deleted in internal storage", oidTreeFile.getName()));
                } else {
                    Log.w(TAG, String.format("file '%s' could not be deleted in internal storage", oidTreeFile.getName()));
                }
            } else {
                Log.w(TAG, String.format("Mib tree file '%s' does not exist", oidTreeFile.getName()));
            }
        }
        // activate default catalog
        activateCatalog(DEFAULT_CATALOG);

        createNewDefaultCatalog();
    }

    public InputStream getCatalogFileInputStream(Context context) throws IOException {
        String currentMibKey = sharedPreferences.getString(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION, null);
        if (currentMibKey == null || currentMibKey.isEmpty() || currentMibKey.equals(DEFAULT_CATALOG)) {
            return context.getAssets().open(MibCatalogArchiveManager.OID_CATALOG_JSON_FILE_NAME);
        }
        return new FileInputStream(new File(context.getFilesDir(),
                currentMibKey + "_" + MibCatalogArchiveManager.OID_CATALOG_JSON_FILE_NAME));
    }

    public InputStream getTreeFileInputStream(Context context) throws IOException {
        String currentMibKey = sharedPreferences.getString(CockpitPreferenceManager.KEY_MIB_CATALOG_SELECTION, null);
        if (currentMibKey == null || currentMibKey.isEmpty() || currentMibKey.equals(DEFAULT_CATALOG)) {
            return context.getAssets().open(MibCatalogArchiveManager.OID_TREE_JSON_FILE_NAME);
        }
        return new FileInputStream(new File(context.getFilesDir(),
                currentMibKey + "_" + MibCatalogArchiveManager.OID_TREE_JSON_FILE_NAME));
    }
}
