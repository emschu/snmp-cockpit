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

package org.emschu.snmp.cockpit.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.adapter.TagListAdapter;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.persistence.model.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * activity to manage tags of this app
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TagManagementActivity extends AppCompatActivity implements ProtectedActivity {

    private TagListAdapter adapter;
    private LinearLayoutManager layoutManager;

    private static final String CURRENT_TAG_INPUT_KEY = "current_tag_input";
    private static final String CURRENT_TAG_ID = "current_tag_id";
    private TagAlertHelper alertHelper;
    private CockpitDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_management);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.activity_title_tag_management);

        RecyclerView recyclerView = findViewById(R.id.tag_recyclerview);

        dbHelper = new CockpitDbHelper(this);
        alertHelper = new TagAlertHelper(this);
        adapter = new TagListAdapter(dbHelper, alertHelper);
        recyclerView.setAdapter(adapter);
        layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);


        initObservables(this, new AlertHelper(this), null);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_tag:
                if (alertHelper != null) {
                    alertHelper.showTagEditDialog(null, false);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (alertHelper != null) {
            outState.putString(CURRENT_TAG_INPUT_KEY, alertHelper.getCurrentTagInput());
            outState.putLong(CURRENT_TAG_ID, alertHelper.getCurrentTagId());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String currentInput = savedInstanceState.getString(CURRENT_TAG_INPUT_KEY);
        long currentId = savedInstanceState.getLong(CURRENT_TAG_ID);

        if (alertHelper == null) {
            alertHelper = new TagAlertHelper(this);
        }
        alertHelper.showTagEditDialog(new Tag(currentId, currentInput), currentId != 0);
    }

    @Override
    public void restartQueryCall() {
        adapter.notifyDataSetChanged();
        layoutManager.scrollToPosition(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startProtection(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restartTrigger(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
