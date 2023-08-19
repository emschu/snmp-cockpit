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
package org.emschu.snmp.cockpit.persistence

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.emschu.snmp.cockpit.model.CustomQuery

/**
 * Query DB Helper
 */
class CockpitDbHelper(context: Context) :
    SQLiteOpenHelper(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "create query table: $QUERY_SQL_CREATE_ENTRIES")
        db.execSQL(QUERY_SQL_CREATE_ENTRIES)

        // insert default stuff
        if (getQueryRowCount(db) == 0) {
            Log.d(TAG, "initial queries are added to app")
            createDefaultEntries(db)
        }
        Log.d(TAG, "inserting default tags finished")
    }

    fun createDefaultEntries(db: SQLiteDatabase) {
        addNewQuery(db, "1.3.6.1.4.1.2021.10.1.3", "laLoad", false)
        addNewQuery(db, "1.3.6.1.4.1.2021.4.3", "memTotalSwap", true)
        addNewQuery(db, "1.3.6.1.4.1.2021.4.4", "memAvailSwap", true)
        addNewQuery(db, "1.3.6.1.4.1.2021.4.5", "memTotalReal", true)
        addNewQuery(db, "1.3.6.1.4.1.2021.4.6", "memAvailReal", true)
        addNewQuery(db, "1.3.6.1.2.1.2.2.1.14", "ifInErrors", true)
        addNewQuery(db, "1.3.6.1.2.1.1.5", "sysName", true)
        addNewQuery(db, "1.3.6.1.2.1.1", "system", false)
    }

    /**
     * if version changes it deletes the existing elements.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "on upgrade query table")
        db.execSQL(QUERY_SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    /**
     * method to count rows
     *
     * @return
     */
    val queryRowCount: Int
        get() = getQueryRowCount(readableDatabase)

    private fun getQueryRowCount(db: SQLiteDatabase): Int {
        val cursor = db.rawQuery("select COUNT(*) from `" + DatabaseContract.QueryTable.TABLE_NAME + "`", null)
        cursor.use { connection ->
            connection.moveToFirst()
            val count = connection.getInt(0)
            return count
        }
    }

    /**
     * is used in adapter
     *
     * @param entryId
     * @return
     */
    fun getCustomQueryById(entryId: Long): CustomQuery? {
        val cursor = readableDatabase.rawQuery(
            "select * from `" + DatabaseContract.QueryTable.TABLE_NAME + "` where _id = $entryId", null
        )
        cursor.use { connection ->
            val works = connection.moveToFirst()
            if (!works) {
                return null
            }

            @SuppressLint("Range") val stringOID =
                connection.getString(connection.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_OID))
            @SuppressLint("Range") val stringName =
                connection.getString(connection.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_NAME))
            @SuppressLint("Range") val isSingleQuery =
                connection.getInt(connection.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE)) == 1
            @SuppressLint("Range") val isInDetails =
                connection.getInt(connection.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_IS_IN_DETAILS)) == 1
            return CustomQuery(
                connection.getInt(0).toLong(), stringOID, stringName, isSingleQuery, isInDetails
            )
        }
    }

    /**
     * method to fetch a list of all CustomQuery instances which are stored in the database
     */
    @Synchronized
    fun getCustomQueries(where: String = ""): List<CustomQuery> {
        val customQueryList = mutableListOf<CustomQuery>()
        val db = readableDatabase
        if (getQueryRowCount(db) == 0) {
            return emptyList()
        }
        val whereClause = if (where.isNotBlank()) {
            "WHERE $where"
        } else {
            ""
        }
        val c = db.rawQuery(
            "select * from `" + DatabaseContract.QueryTable.TABLE_NAME + "` $whereClause order by `" + DatabaseContract.QueryTable._ID + "` desc",
            null
        )
        try {
            c.moveToFirst()
            // this cursor is assumed to have at least one row
            do {
                if (c.count == 0) {
                    // if the result is empty, we cannot retrieve something
                    break
                }
                @SuppressLint("Range") val stringOID =
                    c.getString(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_OID))
                @SuppressLint("Range") val stringName =
                    c.getString(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_NAME))
                @SuppressLint("Range") val isSingleQuery =
                    c.getInt(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE)) == 1
                @SuppressLint("Range") val isInDetails =
                    c.getInt(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_IS_IN_DETAILS)) == 1
                customQueryList.add(
                    CustomQuery(
                        c.getInt(0).toLong(), stringOID, stringName, isSingleQuery, isInDetails
                    )
                )
            } while (c.moveToNext())
        } finally {
            c.close()
            db.close()
        }
        return customQueryList
    }

    fun getCustomQueriesForTab(): List<CustomQuery> {
        return getCustomQueries("${DatabaseContract.QueryTable.COLUMN_NAME_IS_IN_DETAILS} = 1")
    }

    /**
     * method to remove a custom query record by id
     *
     * @param id
     */
    fun removeQuery(id: Long) {
        Log.d(TAG, "remove query: $id")
        writableDatabase.delete(
            DatabaseContract.QueryTable.TABLE_NAME, DatabaseContract.QueryTable._ID + "=" + id, null
        )
    }

    /**
     * add new query record
     *
     * @param inputOID
     * @param inputName
     * @param isSingleQuery
     */
    fun addNewQuery(inputOID: String, inputName: String, isSingleQuery: Boolean, isInDetails: Boolean = true): Long {
        return addNewQuery(writableDatabase, inputOID, inputName, isSingleQuery, isInDetails)
    }

    /**
     * method to add a new query record
     */
    fun addNewQuery(customQuery: CustomQuery) {
        this.addNewQuery(customQuery.oid, customQuery.name, customQuery.isSingleQuery, customQuery.isShowInDetailsTab)
    }

    /**
     * method to add a new query
     *
     * @param db
     * @param inputOID
     * @param inputName
     * @param isSingleQuery
     * @return
     */
    private fun addNewQuery(
        db: SQLiteDatabase, inputOID: String, inputName: String, isSingleQuery: Boolean, isInDetails: Boolean = true,
    ): Long {
        Log.d(TAG, "insert new entry with oid:$inputOID, inputName:$inputName isSingleQuery:$isSingleQuery")
        val values = ContentValues()
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_OID, inputOID.trim())
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_NAME, inputName.trim())
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE, if (isSingleQuery) "1" else "0")
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_IS_IN_DETAILS, if (isInDetails) "1" else "0")
        return db.insert(DatabaseContract.QueryTable.TABLE_NAME, null, values)
    }

    /**
     * method to update a custom query record back to db
     *
     * @param updatedCustomQuery
     */
    fun updateQuery(updatedCustomQuery: CustomQuery) {
        Log.d(TAG, "update query record: " + updatedCustomQuery.id)
        // update query record
        val cv = ContentValues()
        cv.put(DatabaseContract.QueryTable.COLUMN_NAME_NAME, updatedCustomQuery.name.trim())
        cv.put(DatabaseContract.QueryTable.COLUMN_NAME_OID, updatedCustomQuery.oid.trim())
        cv.put(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE, if (updatedCustomQuery.isSingleQuery) "1" else "0")
        cv.put(
            DatabaseContract.QueryTable.COLUMN_NAME_IS_IN_DETAILS,
            if (updatedCustomQuery.isShowInDetailsTab) "1" else "0"
        )
        writableDatabase.update(
            DatabaseContract.QueryTable.TABLE_NAME, cv, DatabaseContract.QueryTable._ID + " = ?",
            arrayOf(updatedCustomQuery.id.toString())
        )
    }

    companion object {
        private const val DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS %s"

        // custom query table
        val QUERY_SQL_CREATE_ENTRIES = String.format(
            "CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s INTEGER, %s INTEGER);",
            DatabaseContract.QueryTable.TABLE_NAME,
            DatabaseContract.QueryTable._ID,
            DatabaseContract.QueryTable.COLUMN_NAME_OID,
            DatabaseContract.QueryTable.COLUMN_NAME_NAME,
            DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE,
            DatabaseContract.QueryTable.COLUMN_NAME_IS_IN_DETAILS,
        )
        private val QUERY_SQL_DELETE_ENTRIES =
            String.format(DROP_TABLE_IF_EXISTS, DatabaseContract.QueryTable.TABLE_NAME)
        private val TAG = CockpitDbHelper::class.java.name
    }
}