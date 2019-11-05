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

package org.emschu.snmp.cockpit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.emschu.snmp.cockpit.query.json.JsonCatalogItem;

public class JsonCatalogTest {

    @Test
    public void testJsonCatalogRead() throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        InputStream resourceAsStream = getClass().getResourceAsStream("/oid_catalog.json");

        Map<String, JsonCatalogItem> map = om.readValue(resourceAsStream, new TypeReference<Map<String, JsonCatalogItem>>(){});
        Assert.assertNotNull(map);
        Assert.assertNotEquals(0, map.size());
        Assert.assertEquals(1341, map.size());
    }

    @Test
    public void testJsonTreeRead() throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        InputStream resourceAsStream = getClass().getResourceAsStream("/oid_tree.json");

        JsonNode node = om.readTree(resourceAsStream);
        Assert.assertNotNull(node);
    }
}
