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

import android.util.Log
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.emschu.snmp.cockpit.SnmpCockpitApp
import org.emschu.snmp.cockpit.SnmpCockpitApp.Companion.context
import org.emschu.snmp.cockpit.snmp.json.JsonCatalogItem
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * central singleton class to manage oid catalog items read by a json file
 */
object OIDCatalog {
    lateinit var mibCatalogManager: MibCatalogManager

    // oid to catalog item
    private val mapOidKey = ConcurrentHashMap<String, JsonCatalogItem>()

    init {
        // initial setup
        if (SnmpCockpitApp.preferenceManager != null) {
            val mcm = MibCatalogManager(SnmpCockpitApp.preferenceManager().sharedPreferences)
            load(mcm)
        }
    }

    /**
     * load data from file into internal catalog
     */
    private fun initData() {
        Log.d(TAG, "reading oid catalog file into catalog")
        val om = ObjectMapper()
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        try {
            if (context == null) {
                return
            }
            mibCatalogManager.getCatalogFileInputStream(context!!)
                .buffered()
                .use { resourceAsStream ->
                    val map = om.readValue(
                        resourceAsStream,
                        object : TypeReference<Map<String, JsonCatalogItem>>() {})
                    for (ci in map.values) {
                        mapOidKey[ci.oid] = ci
                    }
                    Log.d(TAG, "filled oid catalog with " + mapOidKey.size + " values")
                }
        } catch (e: IOException) {
            Log.e(TAG, (if (e.message != null) e.message else "undefined IOException found")!!)
        }
    }

    fun getMapOidKey(): ConcurrentMap<String, JsonCatalogItem> {
        return mapOidKey
    }

    /**
     * get asn name of an oid and strip last number (usually index) of oid for catalog lookup
     *
     * @param oid
     * @return
     */
    fun getAsnByOid(oid: String): String? {
        var key = oid
        while (!mapOidKey.containsKey(key) && key.contains('.')) {
            key = oid.substring(0, key.lastIndexOf('.'))
        }
        if (key == "" || !mapOidKey.containsKey(key)) {
            return ""
        }
        val jsonCatalogItem = mapOidKey[key]
        return jsonCatalogItem?.name
    }

    fun load(mibCatalogManager: MibCatalogManager) {
        this.mibCatalogManager = mibCatalogManager
        initData()
    }

    private val TAG = OIDCatalog::class.java.name
}