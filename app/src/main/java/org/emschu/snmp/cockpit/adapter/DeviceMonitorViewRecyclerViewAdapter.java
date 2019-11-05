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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.fragment.DeviceMonitorViewFragment.OnListFragmentInteractionListener;
import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent.DeviceMonitorItem;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DeviceMonitorItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class DeviceMonitorViewRecyclerViewAdapter extends RecyclerView.Adapter<DeviceMonitorViewRecyclerViewAdapter.DeviceItemViewHolder> {

    public static final String TAG = DeviceMonitorViewRecyclerViewAdapter.class.getName();
    private final List<DeviceMonitorItem> mValues;
    private final OnListFragmentInteractionListener mListener;

    public DeviceMonitorViewRecyclerViewAdapter(List<DeviceMonitorItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public DeviceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_device_card_item, parent, false);
        return new DeviceItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final DeviceItemViewHolder holder, int position) {
        if (mValues.isEmpty()) {
            Log.w(TAG, "empty device item view holder");
            return;
        }
        DeviceMonitorItem deviceMonitorItem = mValues.get(position);
        holder.setDeviceMonitorItem(deviceMonitorItem);
        if (deviceMonitorItem.systemQuery == null) {
            Log.e(TAG, "null system query detected!");
            return;
        }
        String sysName = deviceMonitorItem.systemQuery.getSysName();
        holder.mIdView.setText(String.format("%s %s",
                deviceMonitorItem.id, deviceMonitorItem.deviceConfiguration.getListLabel(sysName)));
        Log.i(TAG, deviceMonitorItem.deviceConfiguration.getSnmpVersionEnum().toString());
        holder.sysIpTextView.setText(deviceMonitorItem.deviceConfiguration.getTargetIp());
        holder.snmpVersionTextView
                .setText(deviceMonitorItem.deviceConfiguration.getSnmpVersionEnum().toString());

        showOrHide(deviceMonitorItem.systemQuery.getSysDescr(), holder.sysDescrTextView, holder.tableRow5sysDescr);
        showOrHide(deviceMonitorItem.systemQuery.getSysLocation(), holder.sysLocationTextView, holder.tableRow6sysLocation);
        showOrHide(sysName, holder.sysNameTextView, holder.tableRow4sysName);
        showOrHide(deviceMonitorItem.systemQuery.getSysContact(), holder.sysContactTextView, holder.tableRow7sysContact);
        showOrHide(deviceMonitorItem.systemQuery.getSysObjectId(), holder.sysObjectIdTextView, holder.tableRow8objectId);
        showOrHide(deviceMonitorItem.systemQuery.getSysUpTime(), holder.sysUpTimeTextView, holder.tableRow9sysUpTime);
        showOrHide(deviceMonitorItem.systemQuery.getSysServices(), holder.sysServicesTextView, holder.tableRow10sysServices);

        holder.mView.setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onListFragmentInteraction(holder.getDeviceMonitorItem());
            }
        });
    }

    private void showOrHide(@Nullable String value, TextView tv, TableRow tr) {
        if (value == null || value.isEmpty()) {
            tr.setVisibility(View.GONE);
        }else {
            tv.setText(value);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * viewholder class for recycler view
     */
    public class DeviceItemViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        private final TextView sysDescrTextView;
        private final TextView sysLocationTextView;
        private final TextView sysNameTextView;
        private final TextView sysContactTextView;
        private final TextView sysObjectIdTextView;
        private final TextView sysUpTimeTextView;
        private final TextView sysServicesTextView;
        private final TextView sysIpTextView;
        private DeviceMonitorItem deviceMonitorItem;
        public final TextView snmpVersionTextView;
        public final TableRow tableRow1;
        public final TableRow tableRow2;
        public final TableRow tableRow3;
        public final TableRow tableRow4sysName;
        public final TableRow tableRow5sysDescr;
        public final TableRow tableRow6sysLocation;
        public final TableRow tableRow7sysContact;
        public final TableRow tableRow8objectId;
        public final TableRow tableRow9sysUpTime;
        public final TableRow tableRow10sysServices;

        public DeviceItemViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = view.findViewById(R.id.item_number);
            snmpVersionTextView = view.findViewById(R.id.device_monitor_text_snmp_version);
            sysDescrTextView = view.findViewById(R.id.device_monitor_sys_description);
            sysLocationTextView = view.findViewById(R.id.device_monitor_sys_location);
            sysNameTextView = view.findViewById(R.id.device_monitor_sys_name);
            sysIpTextView = view.findViewById(R.id.device_monitor_sys_ip);
            sysContactTextView = view.findViewById(R.id.device_monitor_sys_contact);
            sysObjectIdTextView = view.findViewById(R.id.device_monitor_object_id);
            sysUpTimeTextView = view.findViewById(R.id.device_monitor_sys_uptime);
            sysServicesTextView = view.findViewById(R.id.device_monitor_sys_services);

            tableRow1 = view.findViewById(R.id.tablerow_item_1);
            tableRow2 = view.findViewById(R.id.tablerow_item_2);
            tableRow3 = view.findViewById(R.id.tablerow_item_3);
            tableRow4sysName = view.findViewById(R.id.tablerow_item_4_sysName);
            tableRow5sysDescr = view.findViewById(R.id.tablerow_item_5_sysDescr);
            tableRow6sysLocation = view.findViewById(R.id.tablerow_item_6_SysLocation);
            tableRow7sysContact = view.findViewById(R.id.tablerow_item_7_sysContact);
            tableRow8objectId = view.findViewById(R.id.tablerow_item_8_objectId);
            tableRow9sysUpTime = view.findViewById(R.id.tablerow_item_9_sysUpTime);
            tableRow10sysServices = view.findViewById(R.id.tablerow_item_10_sysServices);
        }

        @Override
        public String toString() {
            return "DeviceItemViewHolder{" +
                    "mView=" + mView +
                    ", mIdView=" + mIdView +
                    ", sysDescrTextView=" + sysDescrTextView +
                    ", sysLocationTextView=" + sysLocationTextView +
                    ", sysNameTextView=" + sysNameTextView +
                    ", sysContactTextView=" + sysContactTextView +
                    ", sysObjectIdTextView=" + sysObjectIdTextView +
                    ", sysUpTimeTextView=" + sysUpTimeTextView +
                    ", sysServicesTextView=" + sysServicesTextView +
                    ", sysIpTextView=" + sysIpTextView +
                    ", deviceMonitorItem=" + deviceMonitorItem +
                    ", snmpVersionTextView=" + snmpVersionTextView +
                    '}';
        }

        public DeviceMonitorItem getDeviceMonitorItem() {
            return deviceMonitorItem;
        }

        public void setDeviceMonitorItem(DeviceMonitorItem deviceMonitorItem) {
            this.deviceMonitorItem = deviceMonitorItem;
        }
    }
}
