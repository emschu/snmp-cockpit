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

package org.emschu.snmp.cockpit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.TagAlertHelper;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.persistence.model.Tag;

/**
 * CustomQueryAdapter to get the text and create it.
 */
public class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.QueryAdapterViewHolder> {
    // get table information
    private final CockpitDbHelper cockpitDbHelper;
    private final TagAlertHelper tagAlertHelper;

    /**
     * constructor
     *
     * @param cockpitDbHelper
     * @param tagAlertHelper
     */
    public TagListAdapter(CockpitDbHelper cockpitDbHelper, TagAlertHelper tagAlertHelper) {
        //Initialising the sql table
        this.cockpitDbHelper = cockpitDbHelper;
        this.tagAlertHelper = tagAlertHelper;
    }

    @NonNull
    @Override
    public QueryAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_tag_card_item, parent, false);
        return new QueryAdapterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QueryAdapterViewHolder holder, int position) {
        // tags are already in record
        Tag tagRecord = cockpitDbHelper.getTagByListOffset(position);
        holder.tagNameTextView.setText(tagRecord.getName());

        holder.mView.setOnClickListener(v -> {
            tagAlertHelper.showTagEditDialog(tagRecord, true);
        });
    }

    @Override
    public int getItemCount() {
        return cockpitDbHelper.getAllTags().size();
    }

    /**
     * view holder of an item
     */
    public class QueryAdapterViewHolder extends RecyclerView.ViewHolder {
        private View mView;
        private TextView tagNameTextView;
        /**
         * constructor
         *
         * @param itemView
         */
        private QueryAdapterViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            tagNameTextView = itemView.findViewById(R.id.tag_name_text_view);
        }
    }
}
