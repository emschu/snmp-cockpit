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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.network.WifiNetworkManager;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NetworkDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NetworkDetailsFragment extends Fragment {

    public static final String TAG = NetworkDetailsFragment.class.getName();
    private WifiNetworkManager wifiNetworkManager;
    private View view;

    public NetworkDetailsFragment() {
        // Required empty public constructor
    }

    public static NetworkDetailsFragment newInstance() {
        NetworkDetailsFragment fragment = new NetworkDetailsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiNetworkManager = WifiNetworkManager.getInstance(getContext());

        Log.d(TAG, "update network information");
        wifiNetworkManager.refresh(); // refreshes dhcp info
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_network_details, container, false);

        updateData();

        return view;
    }

    private void updateData() {
        if (view == null) {
            Log.d(TAG, "null view");
            return;
        }
        ((TextView) view.findViewById(R.id.net_details_ip_textview_value))
                .setText(wifiNetworkManager.getIpAddress());
        ((TextView) view.findViewById(R.id.net_details_ssid_textview_value))
                .setText(wifiNetworkManager.getCurrentSsid());
        ((TextView) view.findViewById(R.id.net_details_subnet_textview_value))
                .setText(wifiNetworkManager.getSubnetMask());
        ((TextView) view.findViewById(R.id.net_details_dns_textview_value))
                .setText(wifiNetworkManager.getDNSServer());
        ((TextView) view.findViewById(R.id.net_details_gateway_textview_value))
                .setText(wifiNetworkManager.getGateway());
        ((TextView) view.findViewById(R.id.net_details_ipv6_textview_value))
                .setText(wifiNetworkManager.getIpv6Addresses());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.network_details_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_dhcp) {
            wifiNetworkManager.refresh();
            updateData();
            Toast.makeText(getActivity(), R.string.net_details_refreshed_toast, Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }
}
