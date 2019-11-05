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

package org.emschu.snmp.cockpit.persistence.model;

import java.util.ArrayList;
import java.util.List;

/**
 * custom query model class
 */
public class CustomQuery {
    private long id;
    private String oid;
    private String name;
    private boolean isSingleQuery;
    private List<Tag> tagList = new ArrayList<>();

    public CustomQuery(long id, String oid, String name, boolean isSingleQuery) {
        this.id = id;
        this.oid = oid;
        this.name = name;
        this.isSingleQuery = isSingleQuery;
    }

    public long getId() {
        return id;
    }

    public String getOid() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public boolean isSingleQuery() {
        return isSingleQuery;
    }

    public void setTagList(List<Tag> tagList) {
        this.tagList = tagList;
    }

    public List<Tag> getTagList() {
        return tagList;
    }

    @Override
    public String toString() {
        return "CustomQuery{" +
                "id=" + id +
                ", oid='" + oid + '\'' +
                ", name='" + name + '\'' +
                ", isSingleQuery=" + isSingleQuery +
                '}';
    }
}
