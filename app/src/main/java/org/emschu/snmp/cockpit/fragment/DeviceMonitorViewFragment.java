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

package org.emschu.snmp.cockpit.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.emschu.snmp.cockpit.adapter.DeviceMonitorViewRecyclerViewAdapter;
import org.emschu.snmp.cockpit.fragment.items.DeviceMonitorItemContent.DeviceMonitorItem;
import org.emschu.snmp.cockpit.snmp.DeviceManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.tasks.RefreshDeviceListTask;

import java.util.concurrent.Executors;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class DeviceMonitorViewFragment extends Fragment {

    public static final String TAG = DeviceMonitorViewFragment.class.getName();
    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private DeviceMonitorViewRecyclerViewAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DeviceMonitorViewFragment() {
        // android needs an empty constructor
    }

    @SuppressWarnings("unused")
    public static DeviceMonitorViewFragment newInstance() {
        DeviceMonitorViewFragment fragment = new DeviceMonitorViewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devicemonitorview_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            LinearLayoutManager layout;
            // layout manager depending on orientation
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                layout = new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
            } else {
                layout = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
            }
            recyclerView.setLayoutManager(layout);
            adapter = new DeviceMonitorViewRecyclerViewAdapter(DeviceManager.getInstance().getDeviceList(), mListener);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }

    /**
     * refresh device list + system queries of it
     */
    public void refreshListAsync() {
        Log.d(TAG, "refresh device list");

        OneTimeWorkRequest build = new OneTimeWorkRequest.Builder(RefreshDeviceListTask.class).build();
        WorkManager.getInstance(getActivity()).enqueue(build).getResult().addListener(() -> {
            getActivity().runOnUiThread(() -> {
                if (recyclerView != null) {
                    this.recyclerView.getAdapter().notifyDataSetChanged();
                }
            });
        }, Executors.newSingleThreadExecutor());
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new IllegalStateException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        recyclerView = null;
        adapter = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.main_options_menu, menu);
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(DeviceMonitorItem item);
    }
}
