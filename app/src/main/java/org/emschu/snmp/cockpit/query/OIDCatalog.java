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

package org.emschu.snmp.cockpit.query;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.emschu.snmp.cockpit.query.json.JsonCatalogItem;
import org.emschu.snmp.cockpit.snmp.MibCatalogManager;

/**
 * central singleton class to manage oid catalog items read by a json file
 */
public class OIDCatalog {
    public static final String TAG = OIDCatalog.class.getName();
    private static OIDCatalog instance;
    private final Context context;
    private final MibCatalogManager mibCatalogManager;
    // oid to catalog item
    private ConcurrentHashMap<String, JsonCatalogItem> mapOidKey = new ConcurrentHashMap<>();
    // asn to catalog item
    private ConcurrentHashMap<String, JsonCatalogItem> mapAsnKey = new ConcurrentHashMap<>();

    private OIDCatalog(Context context, MibCatalogManager mibCatalogManager) {
        this.context = context;
        this.mibCatalogManager = mibCatalogManager;
        initData();
    }

    /**
     * singleton access method
     *
     * @param context
     * @return
     */
    public static OIDCatalog getInstance(Context context, MibCatalogManager mibCatalogManager) {
        if (instance == null) {
            if (context == null || mibCatalogManager == null) {
                throw new IllegalArgumentException("null context or mib catalog manager given!");
            }
            instance = new OIDCatalog(context, mibCatalogManager);
        }
        return instance;
    }

    /**
     * load data from file into internal catalog
     */
    private void initData() {
        Log.d(TAG, "reading oid catalog file into catalog");
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (BufferedInputStream resourceAsStream
                     = new BufferedInputStream(mibCatalogManager.getCatalogFileInputStream(context))) {
            Map<String, JsonCatalogItem> map
                    = om.readValue(resourceAsStream, new TypeReference<Map<String, JsonCatalogItem>>() {
            });
            for (JsonCatalogItem ci : map.values()) {
                mapOidKey.put(ci.getOid(), ci);
                mapAsnKey.put(ci.getName(), ci);
            }
            Log.d(TAG, "filled oid catalog with " + mapAsnKey.size() + " values");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public ConcurrentMap<String, JsonCatalogItem> getMapOidKey() {
        return mapOidKey;
    }

    public ConcurrentMap<String, JsonCatalogItem> getMapAsnKey() {
        return mapAsnKey;
    }

    /**
     * get asn name of an oid and strip last number (usually index) of oid for catalog lookup
     *
     * @param oid
     * @return
     */
    public String getAsnByOidStripLast(String oid) throws OIDNotInCatalogException {
        String key = oid.substring(0, oid.lastIndexOf('.'));
        if (!mapOidKey.containsKey(key)) {
            // try to strip last 4 numbers (ip addr index of snmp)
            // TODO this is not elegant here!
            key = oid.substring(0, oid.lastIndexOf('.'));
            key = oid.substring(0, key.lastIndexOf('.'));
            if (mapOidKey.containsKey(key)) {
                return mapOidKey.get(key).getName();
            }
            key = oid.substring(0, key.lastIndexOf('.'));
            key = oid.substring(0, key.lastIndexOf('.'));
            if (!mapOidKey.containsKey(key)) {
                key = oid.substring(0, key.lastIndexOf('.'));
            }
            if (!mapOidKey.containsKey(key)) {
                throw new OIDNotInCatalogException("could not find oid: " + key);
            }
        }
        JsonCatalogItem jsonCatalogItem = mapOidKey.get(key);
        return jsonCatalogItem.getName();
    }

    /**
     * get oid by asn name
     *
     * @param asnName
     * @return
     */
    public String getOidByAsn(String asnName) {
        if (mapAsnKey.containsKey(asnName)) {
            return mapAsnKey.get(asnName).getOid();
        }
        return null;
    }

    public void refresh() {
        // reload the whole stuff with proper files
        instance = null;
        OIDCatalog.getInstance(context, mibCatalogManager);
    }
}
