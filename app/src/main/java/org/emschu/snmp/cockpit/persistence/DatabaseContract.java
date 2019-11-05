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

package org.emschu.snmp.cockpit.persistence;

import android.provider.BaseColumns;

/**
 * this class defines the database structure
 */
public class DatabaseContract {
    public static final String DATABASE_NAME = "SNMPCockpit.db";
    public static final int DATABASE_VERSION = 21;

    /**
     * query table
     */
    public class QueryTable implements BaseColumns {
        static final String TABLE_NAME = "custom_queries";

        static final String COLUMN_NAME_OID = "OID";
        static final String COLUMN_NAME_NAME = "description";
        static final String COLUMN_NAME_IS_SINGLE = "is_single";
    }

    /**
     * category table
     */
    public class CategoryTable implements BaseColumns {
        static final String TABLE_NAME = "tags";

        static final String COLUMN_NAME_NAME = "description";
    }

    /**
     * linking table for m:n relation of the tables above
     */
    public class QueryToCategoryTable implements BaseColumns {
        static final String TABLE_NAME = "query_to_category";

        static final String COLUMN_NAME_QUERY_ID = "query_id";
        static final String COLUMN_NAME_CATEGORY_ID = "category_id";
    }
}
