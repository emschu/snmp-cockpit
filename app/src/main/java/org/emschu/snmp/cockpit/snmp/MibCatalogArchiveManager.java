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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.emschu.snmp.cockpit.query.json.JsonCatalogItem;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * this class manages handling of "uploaded" .zip archives for a new internal mib catalog
 */
public class MibCatalogArchiveManager {

    public static final String TAG = MibCatalogArchiveManager.class.getName();
    public static final String OID_CATALOG_JSON_FILE_NAME = "oid_catalog.json";
    public static final String OID_TREE_JSON_FILE_NAME = "oid_tree.json";
    private final Activity context;
    private final Uri locationToZip;
    private final String archiveName;

    public MibCatalogArchiveManager(@NotNull Activity context, @NotNull Uri locationToZip) {
        this.context = context;
        this.locationToZip = locationToZip;
        this.archiveName = getArchiveName(locationToZip);
    }

    public String getArchiveName() {
        return archiveName;
    }

    public final boolean isArchiveValid() {
        boolean hasCatalogFile = false;
        boolean hasTreeFile = false;

        String fileName;
        try (InputStream is = context.getContentResolver().openInputStream(locationToZip)) {

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
                ZipEntry ze;

                while ((ze = zis.getNextEntry()) != null) {
                    fileName = ze.getName();

                    if (fileName.equals(OID_CATALOG_JSON_FILE_NAME)) {
                        hasCatalogFile = true;
                    } else if (fileName.equals(OID_TREE_JSON_FILE_NAME)) {
                        hasTreeFile = true;
                    } else {
                        Log.e(TAG, String.format("invalid file name '%s' in import zip file", fileName));
                        return false;
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, String.format("IoException: '%s'", e.getMessage()));
            return false;
        }
        return hasCatalogFile && hasTreeFile;
    }

    /**
     * @return success state
     */
    public final boolean unpackZip() {
        String fileName;
        try (InputStream is = context.getContentResolver().openInputStream(locationToZip)) {

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
                ZipEntry ze;

                while ((ze = zis.getNextEntry()) != null) {
                    fileName = ze.getName();

                    if (!fileName.equals(OID_CATALOG_JSON_FILE_NAME)
                            && !fileName.equals(OID_TREE_JSON_FILE_NAME)) {
                        Log.w(TAG, String.format("unexpected file '%s' in import mib archive", fileName));
                        continue;
                    }

                    // write each single file
                    writeFileToInternalStorage(zis, fileName, archiveName);

                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IoException during zip unpacking:" + e.getMessage());
            return false;
        }

        return true;
    }

    private void writeFileToInternalStorage(ZipInputStream zis, String fileName, String catalogName) throws IOException {
        int count;
        byte[] buffer = new byte[1024];
        String internalFileName = catalogName + "_" + fileName;
        Log.d(TAG, "internal file name: " + internalFileName);

        File internalFile = new File(context.getFilesDir(), internalFileName);
        try (FileOutputStream fout = new FileOutputStream(internalFile)) {
            while ((count = zis.read(buffer)) != -1) {
                fout.write(buffer, 0, count);
            }

            // check that jackson can read this json
            try (FileInputStream fis = new FileInputStream(internalFile)) {
                ObjectMapper om = new ObjectMapper();
                if (internalFileName.contains(OID_CATALOG_JSON_FILE_NAME)) {
                    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    om.readValue(fis, new TypeReference<Map<String, JsonCatalogItem>>() {
                    });
                } else {
                    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
                    Reader is = new BufferedReader(new FileReader(internalFile));
                    om.readTree(is);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "io exception during processing .zip file");
            throw new IOException(e);
        }
    }

    @NotNull
    private String getArchiveName(@NotNull Uri locationToZip) {
        String detectedArchiveName = "";
        if (locationToZip.getScheme() != null && locationToZip.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver()
                    .query(locationToZip, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    detectedArchiveName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        } else {
            String lastPathSegment = locationToZip.getLastPathSegment();
            if (lastPathSegment != null) {
                detectedArchiveName = new File(lastPathSegment).getName();
            } else {
                Log.w(TAG, "No last path segment in Uri found");
            }
        }
        Log.i(TAG, String.format("import archive name '%s'", detectedArchiveName));
        return detectedArchiveName.replace(".zip", "");
    }

}
