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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.emschu.snmp.cockpit.persistence.model.CustomQuery;
import org.emschu.snmp.cockpit.persistence.model.Tag;

/**
 * Query DB Helper
 */
public class CockpitDbHelper extends SQLiteOpenHelper {

    // category table
    public static final String CATEGORY_SQL_CREATE_ENTRIES =
            String.format("CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT);",
                    DatabaseContract.CategoryTable.TABLE_NAME,
                    DatabaseContract.CategoryTable._ID,
                    DatabaseContract.CategoryTable.COLUMN_NAME_NAME);

    public static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS %s";
    private static final String CATEGORY_SQL_DELETE_ENTRIES =
            String.format(DROP_TABLE_IF_EXISTS, DatabaseContract.CategoryTable.TABLE_NAME);

    // custom query table
    public static final String QUERY_SQL_CREATE_ENTRIES =
            String.format("CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s INTEGER);",
                    DatabaseContract.QueryTable.TABLE_NAME,
                    DatabaseContract.QueryTable._ID,
                    DatabaseContract.QueryTable.COLUMN_NAME_OID,
                    DatabaseContract.QueryTable.COLUMN_NAME_NAME,
                    DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE);

    private static final String QUERY_SQL_DELETE_ENTRIES =
            String.format(DROP_TABLE_IF_EXISTS, DatabaseContract.QueryTable.TABLE_NAME);
    public static final String TAG = CockpitDbHelper.class.getName();

    // relation table
    public static final String QUERY_TO_CATALOG_TABLE_SQL_CREATE_ENTRIES =
            String.format("CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER, %s INTEGER);",
                    DatabaseContract.QueryToCategoryTable.TABLE_NAME,
                    DatabaseContract.QueryToCategoryTable._ID,
                    DatabaseContract.QueryToCategoryTable.COLUMN_NAME_QUERY_ID,
                    DatabaseContract.QueryToCategoryTable.COLUMN_NAME_CATEGORY_ID);

    private static final String QUERY_TO_CATALOG_TABLE_SQL_DELETE_ENTRIES =
            String.format(DROP_TABLE_IF_EXISTS, DatabaseContract.QueryToCategoryTable.TABLE_NAME);

    /**
     * constructor
     *
     * @param context
     */
    public CockpitDbHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "create query table: " + QUERY_SQL_CREATE_ENTRIES);
        Log.d(TAG, "create category table: " + CATEGORY_SQL_CREATE_ENTRIES);
        Log.d(TAG, "create relation table: " + QUERY_TO_CATALOG_TABLE_SQL_CREATE_ENTRIES);
        db.execSQL(QUERY_SQL_CREATE_ENTRIES);
        db.execSQL(CATEGORY_SQL_CREATE_ENTRIES);
        db.execSQL(QUERY_TO_CATALOG_TABLE_SQL_CREATE_ENTRIES);

        // insert default stuff
        String[] defaultTags = new String[]{
                "Router", "Switch", "Firewall", "Storage", "NAS", "Cluster", "RAID", "HA", "GPU", "CPU"
        };
        for (String singleTag : defaultTags) {
            addNewTag(db, singleTag);
        }
        if (getQueryRowCount(db) == 0) {
            Log.d(TAG, "initial queries are added to app");
            addNewQuery(db, "1.3.6.1.4.1.2021.10.1.3", "laLoad", false);
            addNewQuery(db, "1.3.6.1.4.1.2021.4.3", "memTotalSwap", true);
            addNewQuery(db, "1.3.6.1.4.1.2021.4.4", "memAvailSwap", true);
            addNewQuery(db, "1.3.6.1.4.1.2021.4.5", "memTotalReal", true);
            addNewQuery(db, "1.3.6.1.4.1.2021.4.6", "memAvailReal", true);
            addNewQuery(db, "1.3.6.1.2.1.2.2.1.14", "ifInErrors", true);
            addNewQuery(db, "1.3.6.1.2.1.1.5", "sysName", true);
            addNewQuery(db, "1.3.6.1.2.1.1", "system", false);
        }
        Log.d(TAG, "inserting default tags finished");
    }

    /**
     * if version changes it deletes the existing elements.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "on upgrade query table");
        db.execSQL(QUERY_TO_CATALOG_TABLE_SQL_DELETE_ENTRIES);
        db.execSQL(CATEGORY_SQL_DELETE_ENTRIES);
        db.execSQL(QUERY_SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    /**
     * method to count rows
     *
     * @return
     */
    public int getQueryRowCount() {
        return getQueryRowCount(getReadableDatabase());
    }

    private int getQueryRowCount(SQLiteDatabase db) {
        Cursor c = db.rawQuery("select COUNT(*) from `" + DatabaseContract.QueryTable.TABLE_NAME + "`", null);
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        return count;
    }

    /**
     * is used in adapter
     *
     * @param position
     * @return
     */
    public CustomQuery getCustomQueryByListOffset(int position) {
        Cursor c = getReadableDatabase().rawQuery("select * from `" + DatabaseContract.QueryTable.TABLE_NAME + "` order by `"
                + DatabaseContract.QueryTable._ID + "` desc limit 1 offset " + position, null);
        c.moveToFirst();

        String stringOID = c.getString(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_OID));
        String stringName = c.getString(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_NAME));
        boolean isSingleQuery = c.getInt(c.getColumnIndex(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE)) == 1;
        CustomQuery customQueryRecord = new CustomQuery(c.getInt(0), stringOID, stringName, isSingleQuery);
        c.close();

        // add tags
        customQueryRecord.setTagList(getTagsOfQuery(customQueryRecord.getId()));

        return customQueryRecord;
    }

    /**
     * method to remove a custom query record by id
     *
     * @param id
     */
    public void removeQuery(long id) {
        Log.d(TAG, "remove query: " + id);
        getWritableDatabase().delete(DatabaseContract.QueryTable.TABLE_NAME,
                DatabaseContract.QueryTable._ID + "=" + id, null);
    }

    /**
     * add new query record
     *
     * @param inputOID
     * @param inputName
     * @param isSingleQuery
     */
    public long addNewQuery(String inputOID, String inputName, boolean isSingleQuery) {
        return addNewQuery(getWritableDatabase(), inputOID, inputName, isSingleQuery);
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
    private long addNewQuery(SQLiteDatabase db, String inputOID, String inputName, boolean isSingleQuery) {
        Log.d(TAG, "insert new entry with oid:" + inputOID + ", inputName:" + inputName + " isSingleQuery:" + isSingleQuery);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_OID, inputOID.trim());
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_NAME, inputName.trim());
        values.put(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE, isSingleQuery ? "1" : "0");
        return db.insert(DatabaseContract.QueryTable.TABLE_NAME, null, values);
    }

    /**
     * method to get tags of a query
     *
     * @param id
     * @return
     */
    public List<Tag> getTagsOfQuery(long id) {
        List<Long> categoryIdsOfQuery = getCategoryIdsOfQuery(id);
        List<Tag> tagList = new ArrayList<>();
        if (categoryIdsOfQuery.isEmpty()) {
            return tagList;
        }
        Cursor c;
        if (categoryIdsOfQuery.size() > 1) {
            c = getReadableDatabase().query(DatabaseContract.CategoryTable.TABLE_NAME,
                    new String[]{
                            DatabaseContract.CategoryTable._ID,
                            DatabaseContract.CategoryTable.COLUMN_NAME_NAME,
                    },
                    DatabaseContract.CategoryTable._ID + " IN (" + TextUtils.join(",", categoryIdsOfQuery) + ")",
                    null, null, null, null);
        } else {
            c = getReadableDatabase().rawQuery(
                    "SELECT * FROM `" + DatabaseContract.CategoryTable.TABLE_NAME +
                            "` WHERE `" + DatabaseContract.CategoryTable._ID + "` = ? LIMIT 1;",
                    new String[]{String.valueOf(categoryIdsOfQuery.get(0))});
        }

        while (c.moveToNext()) {
            String catName = c.getString(c.getColumnIndex(DatabaseContract.CategoryTable.COLUMN_NAME_NAME));
            long idField = c.getLong(c.getColumnIndex(DatabaseContract.CategoryTable._ID));
            tagList.add(new Tag(idField, catName));
            Log.d(TAG, "category added");
        }
        c.close();

        return tagList;
    }

    /**
     * method to add a new tag/category
     *
     * @param singleTag
     */
    public void addNewTag(SQLiteDatabase db, String singleTag) {
        if (doesTagExist(db, singleTag)) {
            Log.d(TAG, "avoid duplicate tag");
            return;
        }
        Log.d(TAG, "add new tag: " + singleTag);
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.CategoryTable.COLUMN_NAME_NAME, singleTag.trim());
        db.insert(DatabaseContract.CategoryTable.TABLE_NAME, null, cv);
    }

    /**
     * method to get all tags from db as {@link Tag} objects
     *
     * @return
     */
    public List<Tag> getAllTags() {
        List<Tag> tagList = new ArrayList<>();

        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM " + DatabaseContract.CategoryTable.TABLE_NAME, null);

        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndex(DatabaseContract.CategoryTable.COLUMN_NAME_NAME));
            long id = c.getLong(c.getColumnIndex(DatabaseContract.CategoryTable._ID));
            tagList.add(new Tag(id, name));
        }
        c.close();

        return tagList;
    }

    /**
     * link tags to a query identified with its id
     *
     * @param insertedQueryId
     * @param selectedTags
     */
    public void linkTagsToQuery(long insertedQueryId, Tag[] selectedTags) {
        for (Tag singleTag : selectedTags) {
            Log.d(TAG, "linking inserted query " + insertedQueryId + "to tag" + singleTag.getId());

            if (isLinkBetween(insertedQueryId, singleTag.getId())) {
                Log.d(TAG, "duplicate link detected!");
                continue;
            }

            ContentValues cv = new ContentValues();
            cv.put(DatabaseContract.QueryToCategoryTable.COLUMN_NAME_QUERY_ID, insertedQueryId);
            cv.put(DatabaseContract.QueryToCategoryTable.COLUMN_NAME_CATEGORY_ID, singleTag.getId());
            getWritableDatabase().insert(DatabaseContract.QueryToCategoryTable.TABLE_NAME, null, cv);
        }
    }

    /**
     * method to check if there is a relation between a query and a category
     *
     * @param queryId
     * @param categoryId
     * @return
     */
    public boolean isLinkBetween(long queryId, long categoryId) {
        Cursor c = getReadableDatabase().query(
                DatabaseContract.QueryToCategoryTable.TABLE_NAME,
                new String[]{
                        DatabaseContract.QueryToCategoryTable._ID
                },
                DatabaseContract.QueryToCategoryTable.COLUMN_NAME_QUERY_ID + "= ? AND "
                        + DatabaseContract.QueryToCategoryTable.COLUMN_NAME_CATEGORY_ID + "= ?",
                new String[]{
                        String.valueOf(queryId),
                        String.valueOf(categoryId)
                },
                null, null, null, "1");
        c.moveToNext();
        if (c.getCount() == 0) {
            c.close();
            return false;
        }
        c.close();
        return true;
    }

    /**
     * checks if a tag does exist
     *
     * @param db
     * @param tag
     * @return
     */
    public boolean doesTagExist(SQLiteDatabase db, String tag) {
        Cursor c = db.query(
                DatabaseContract.CategoryTable.TABLE_NAME,
                new String[]{
                        DatabaseContract.CategoryTable._ID
                },
                DatabaseContract.CategoryTable.COLUMN_NAME_NAME + " LIKE ?",
                new String[]{
                        tag,
                },
                null, null, null, "1");
        c.moveToNext();
        if (c.getCount() == 0) {
            c.close();
            return false;
        }
        c.close();
        return true;
    }

    /**
     * get list of category ids of a query
     *
     * @param queryId
     * @return
     */
    public List<Long> getCategoryIdsOfQuery(long queryId) {
        Cursor c = getReadableDatabase().rawQuery("select * from `" + DatabaseContract.QueryToCategoryTable.TABLE_NAME + "`" +
                " WHERE `" + DatabaseContract.QueryToCategoryTable.COLUMN_NAME_QUERY_ID + "` = ? order by `"
                + DatabaseContract.QueryTable._ID + "`", new String[]{String.valueOf(queryId)});

        List<Long> categoryList = new ArrayList<>();
        while (c.moveToNext()) {
            long aLong = c.getLong(c.getColumnIndex(DatabaseContract.QueryToCategoryTable.COLUMN_NAME_CATEGORY_ID));
            categoryList.add(aLong);
        }
        Log.d(TAG, "detected " + categoryList.size() + " categories:" + categoryList);
        c.close();
        return categoryList;
    }

    /**
     * method to update a custom query record back to db
     *
     * @param updatedCustomQuery
     */
    public void updateQuery(CustomQuery updatedCustomQuery) {
        if (updatedCustomQuery == null) {
            throw new NullPointerException("null query given!");
        }
        Log.d(TAG, "update query record: " + updatedCustomQuery.getId());

        // delete all tag links and re-create them afterwards
        getWritableDatabase().delete(
                DatabaseContract.QueryToCategoryTable.TABLE_NAME,
                DatabaseContract.QueryToCategoryTable.COLUMN_NAME_QUERY_ID + " = ?",
                new String[]{String.valueOf(updatedCustomQuery.getId())}
        );
        // re-link tags
        linkTagsToQuery(updatedCustomQuery.getId(), updatedCustomQuery.getTagList().toArray(new Tag[]{}));

        // update query record
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.QueryTable.COLUMN_NAME_NAME, updatedCustomQuery.getName().trim());
        cv.put(DatabaseContract.QueryTable.COLUMN_NAME_OID, updatedCustomQuery.getOid().trim());
        cv.put(DatabaseContract.QueryTable.COLUMN_NAME_IS_SINGLE, updatedCustomQuery.isSingleQuery() ? "1" : "0");

        getWritableDatabase().update(
                DatabaseContract.QueryTable.TABLE_NAME,
                cv,
                DatabaseContract.QueryTable._ID + " = ?",
                new String[]{String.valueOf(updatedCustomQuery.getId())}
        );
    }

    /**
     * used in {@link org.emschu.snmp.cockpit.adapter.TagListAdapter}
     *
     * @param position
     * @return
     */
    public Tag getTagByListOffset(int position) {
        Cursor c = getReadableDatabase().rawQuery("select * from `" + DatabaseContract.CategoryTable.TABLE_NAME + "` order by `"
                + DatabaseContract.CategoryTable._ID + "` desc limit 1 offset " + position, null);
        c.moveToFirst();

        long id = c.getLong(c.getColumnIndex(DatabaseContract.CategoryTable._ID));
        String categoryName = c.getString(c.getColumnIndex(DatabaseContract.CategoryTable.COLUMN_NAME_NAME));
        Tag tagRecord = new Tag(id, categoryName);
        c.close();
        return tagRecord;
    }

    /**
     * persist a tag record to db
     *
     * @param updatedTag
     */
    public void updateTag(Tag updatedTag) {
        if (updatedTag == null) {
            throw new NullPointerException("null tag given!");
        }
        Log.d(TAG, "update tag record: " + updatedTag.getId());

        // update query record
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.CategoryTable.COLUMN_NAME_NAME, updatedTag.getName().trim());

        getWritableDatabase().update(
                DatabaseContract.CategoryTable.TABLE_NAME,
                cv,
                DatabaseContract.CategoryTable._ID + " = ?",
                new String[]{String.valueOf(updatedTag.getId())}
        );
    }

    /**
     * method to remove a tag record from db
     *
     * @param tagRecord
     */
    public void removeTag(Tag tagRecord) {
        Log.d(TAG, "remove tag: " + tagRecord.getId());
        // dleete all links to queries
        getWritableDatabase().delete(
                DatabaseContract.QueryToCategoryTable.TABLE_NAME,
                DatabaseContract.QueryToCategoryTable.COLUMN_NAME_CATEGORY_ID + " = ?",
                new String[]{String.valueOf(tagRecord.getId())}
        );

        // delete record itself
        getWritableDatabase().delete(
                DatabaseContract.CategoryTable.TABLE_NAME,
                DatabaseContract.CategoryTable._ID + " = ?",
                new String[]{String.valueOf(tagRecord.getId())}
        );
    }
}
