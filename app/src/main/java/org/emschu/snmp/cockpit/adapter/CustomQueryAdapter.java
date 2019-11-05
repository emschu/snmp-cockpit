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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.emschu.snmp.cockpit.CockpitMainActivity;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.AlertHelper;
import org.emschu.snmp.cockpit.fragment.OwnQueryFragment;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.persistence.model.CustomQuery;
import org.emschu.snmp.cockpit.persistence.model.Tag;

/**
 * CustomQueryAdapter to get the text and create it.
 */
public class CustomQueryAdapter extends RecyclerView.Adapter<CustomQueryAdapter.QueryAdapterViewHolder> {
    // get table information
    private final CockpitDbHelper cockpitDbHelper;
    private final Context context;

    /**
     * constructor
     *
     * @param cockpitDbHelper
     * @param context
     */
    public CustomQueryAdapter(CockpitDbHelper cockpitDbHelper, Context context) {
        //Initialising the sql table
        this.cockpitDbHelper = cockpitDbHelper;
        this.context = context;
    }

    @NonNull
    @Override
    public QueryAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_query_recycler_view_item, parent, false);
        return new QueryAdapterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QueryAdapterViewHolder holder, int position) {
        // tags are already in record
        CustomQuery customQuery = cockpitDbHelper.getCustomQueryByListOffset(position);
        holder.queryOidTextView.setText(customQuery.getOid());
        holder.queryNameTextView.setText(customQuery.getName());
        List<Tag> tagsList = cockpitDbHelper.getTagsOfQuery(customQuery.getId());
        Log.d("adapter", "taglist: " + tagsList.toString());
        if (tagsList.isEmpty()) {
            // hide category field if query has no category
            holder.categoryRow.setVisibility(View.GONE);
        } else {
            holder.categoryRow.setVisibility(View.VISIBLE);
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Tag tag : tagsList) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(tag.getName());
            isFirst = false;
        }
        holder.categoryTextView.setText(sb.toString());
        holder.queryOptionButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.queryOptionButton);
            popupMenu.getMenuInflater().inflate(R.menu.custom_query_single_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.edit_query:
                        if (context instanceof CockpitMainActivity) {
                            OwnQueryFragment ownQueryFragment = ((CockpitMainActivity) context).getOwnQueryFragment();
                            ownQueryFragment.showCustomQueryDialog(customQuery, true);
                        }
                        break;
                    case R.id.show_query:
                        if (context instanceof CockpitMainActivity) {
                            new AlertHelper(context).showQueryTargetDialog(customQuery.getOid());
                        }
                        break;
                    default:
                        break;
                }
                return true;
            });
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        if (cockpitDbHelper != null) {
            return cockpitDbHelper.getQueryRowCount();
        }
        return 0;
    }

    /**
     * view holder of an item
     */
    public class QueryAdapterViewHolder extends RecyclerView.ViewHolder {
        // declaration of the elements for one row.
        private TextView queryOidTextView;
        private TextView queryNameTextView;
        private TextView categoryTextView;
        private ImageButton queryOptionButton;
        private TableRow categoryRow;

        /**
         * constructor
         *
         * @param itemView
         */
        private QueryAdapterViewHolder(View itemView) {
            super(itemView);
            //Initialising elements for one row.
            queryOidTextView = itemView.findViewById(R.id.query_oid_view_field);
            queryNameTextView = itemView.findViewById(R.id.query_name_view_field);
            categoryTextView = itemView.findViewById(R.id.query_category_view_field);
            queryOptionButton = itemView.findViewById(R.id.option_queries);
            categoryRow = itemView.findViewById(R.id.query_card_category_row);
        }
    }
}
