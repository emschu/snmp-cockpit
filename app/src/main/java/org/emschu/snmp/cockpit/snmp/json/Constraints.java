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

package org.emschu.snmp.cockpit.snmp.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * representing constraints node in oid_catalog.json
 */
@JsonIgnoreProperties
public class Constraints {
    @JsonProperty("range")
    private SizeConstraint[] range;

    @JsonProperty("size")
    private SizeConstraint[] size;

    @JsonProperty("enumeration")
    private EnumerationConstraint enumeration;

    public Constraints() {
    }

    @JsonProperty("range")
    public SizeConstraint[] getRange() {
        return range;
    }

    @JsonProperty("range")
    public void setRange(SizeConstraint[] range) {
        this.range = range;
    }

    @JsonProperty("size")
    public SizeConstraint[] getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(SizeConstraint[] size) {
        this.size = size;
    }

    @JsonProperty("enumeration")
    public EnumerationConstraint getEnumeration() {
        return enumeration;
    }

    @JsonProperty("enumeration")
    public void setEnumeration(EnumerationConstraint enumeration) {
        this.enumeration = enumeration;
    }
}
