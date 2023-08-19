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

package org.emschu.snmp.cockpit.snmp.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * representing json catalog item in oid_catalog.json
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "oid",
        "nodetype",
        "class",
        "syntax",
        "maxaccess",
        "status",
        "description"
})
@JsonIgnoreProperties
public class JsonCatalogItem {

    @JsonProperty("name")
    private String name;
    @JsonProperty("oid")
    private String oid;
    @JsonProperty("nodetype")
    private String nodetype;
    @JsonProperty("class")
    private String _class;
    @JsonProperty("syntax")
    private Syntax syntax;
    @JsonProperty("maxaccess")
    private String maxaccess;
    @JsonProperty("status")
    private String status;
    @JsonProperty("description")
    private String description;

    @JsonIgnore()
    private String indices;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("oid")
    public String getOid() {
        return oid;
    }

    @JsonProperty("oid")
    public void setOid(String oid) {
        this.oid = oid;
    }

    @JsonProperty("nodetype")
    public String getNodetype() {
        return nodetype;
    }

    @JsonProperty("nodetype")
    public void setNodetype(String nodetype) {
        this.nodetype = nodetype;
    }

    @JsonProperty("class")
    public String getClass_() {
        return _class;
    }

    @JsonProperty("class")
    public void setClass_(String _class) {
        this._class = _class;
    }

    @JsonProperty("syntax")
    public Syntax getSyntax() {
        return syntax;
    }

    @JsonProperty("syntax")
    public void setSyntax(Syntax syntax) {
        this.syntax = syntax;
    }

    @JsonProperty("maxaccess")
    public String getMaxaccess() {
        return maxaccess;
    }

    @JsonProperty("maxaccess")
    public void setMaxaccess(String maxaccess) {
        this.maxaccess = maxaccess;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }
}
