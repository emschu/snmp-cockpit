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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.AlertHelper;
import org.emschu.snmp.cockpit.query.OIDCatalog;
import org.emschu.snmp.cockpit.snmp.MibCatalog;
import org.emschu.snmp.cockpit.snmp.MibCatalogArchiveManager;
import org.emschu.snmp.cockpit.snmp.MibCatalogManager;

import tellh.com.recyclertreeview_lib.LayoutItemType;
import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewAdapter;
import tellh.com.recyclertreeview_lib.TreeViewBinder;

/**
 * A fragment representing the mib.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class MibCatalogFragment extends Fragment {

    public static final String TAG = MibCatalogFragment.class.getName();
    public static final int READ_REQUEST_CODE = 45;
    private LinearLayoutManager layout;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MibCatalogFragment() {
        // mandatory empty
    }

    /**
     * @return
     */
    public static MibCatalogFragment newInstance() {
        MibCatalogFragment fragment = new MibCatalogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_query_catalog, container, false);

        List<TreeNode> nodes = new ArrayList<>();
        TreeNode<CatalogItem> catalogTree = new TreeNode<>(new CatalogItem(getString(R.string.mib_tree_title)));
        nodes.add(catalogTree);

        JsonNode parentNode = readJsonTree();
        if (parentNode != null) {
            loadJsonTree(parentNode, 0, catalogTree);
        }
        TreeViewAdapter adapter = new TreeViewAdapter(nodes,
                Arrays.asList(new CatalogItemBinder(),
                        new DirectoryNodeBinder(),
                        new TableNodeBinder(),
                        new TableEntryNodeBinder(),
                        new TableEntryItemNodeBinder()
                ));
        RecyclerView rv = view.findViewById(R.id.query_catalog_list);
        layout = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        rv.setLayoutManager(layout);

        adapter.setOnTreeNodeListener(new TreeNodeListener());
        adapter.ifCollapseChildWhileCollapseParent(false);
        rv.setAdapter(adapter);

        if (parentNode == null) {
            return view;
        }

        TreeNode firstNode = catalogTree.getChildList().get(0);
        TreeNode secondNode = (TreeNode) firstNode.getChildList().get(0);
        TreeNode thirdNode = (TreeNode) secondNode.getChildList().get(0);
        catalogTree.expand(); // expand root
        firstNode.expand(); // 1.3
        secondNode.expand(); // 1.3.6
        thirdNode.expand(); // 1.3.6.1
        TreeNode node4 = (TreeNode) thirdNode.getChildList().get(0);
        TreeNode node5 = (TreeNode) thirdNode.getChildList().get(1);
        node4.expand(); // 1.3.6.1.2
        node5.expand(); // 1.3.6.1.6
        adapter.refresh(nodes);

        return view;
    }

    /**
     * heart of the tree
     *
     * @param parentNode
     * @param depth
     * @param parentTreeNode
     */
    private static void loadJsonTree(JsonNode parentNode, int depth, TreeNode<CatalogItem> parentTreeNode) {
        if (!parentNode.isObject()) {
            throw new IllegalStateException("invalid input given. no json object in " + parentNode);
        }
        if (parentTreeNode == null) {
            throw new IllegalStateException("invalid parent tree node given");
        }
        JsonNode childrenNodes = parentNode.path("children");
        if (childrenNodes == null || !childrenNodes.isObject()) {
            throw new IllegalStateException("invalid children object received in json oid tree");
        }
        Iterator<JsonNode> iter = childrenNodes.iterator();
        Iterator<String> fieldNames = childrenNodes.fieldNames();
        while (iter.hasNext()) {
            JsonNode node = iter.next();
            String fieldName = fieldNames.next();
            // affects "children" fields
            boolean isLeaf = node.path("isLeaf").asBoolean();
            if (isLeaf) {
                processLeaf(depth, parentTreeNode, childrenNodes, node);
            } else {
                if (fieldName.equals(".") || fieldName.equals("1")) {
                    loadJsonTree(node, depth, parentTreeNode);
                } else {
                    TreeNode<CatalogItem> subTree = new TreeNode<>(new CatalogItem(fieldName));
                    parentTreeNode.addChild(subTree);
                    loadJsonTree(node, depth + 1, subTree);
                }
            }
        }
    }

    /**
     * helper method to process a single leaf tree node
     *
     * @param depth
     * @param parentTreeNode
     * @param childrenNodes
     * @param node
     */
    private static void processLeaf(int depth, TreeNode<CatalogItem> parentTreeNode, JsonNode childrenNodes, JsonNode node) {
        if (parentTreeNode == null) {
            throw new IllegalStateException("invalid parent tree node");
        }
        String nodeAsnName = node.path("name").textValue();
        String oidValueOfLeaf = node.path("oidValue").textValue();
        String leafNodeTitle = oidValueOfLeaf + " - " + nodeAsnName;
        TreeNode<CatalogItem> subTree;
        if (nodeAsnName.endsWith("Table")) {
            subTree = new TreeNode<>(new CatalogTable(leafNodeTitle), oidValueOfLeaf, leafNodeTitle);
        } else if (nodeAsnName.endsWith("Entry")) {
            subTree = new TreeNode<>(new CatalogTableEntry(leafNodeTitle), oidValueOfLeaf, leafNodeTitle);
        } else {
            subTree = new TreeNode<>(new CatalogItem(leafNodeTitle), oidValueOfLeaf, leafNodeTitle);
        }
        JsonNode childrenOfLeaf = node.path("children");
        if (!childrenNodes.isObject()) {
            throw new IllegalStateException("invalid children object received in json oid tree");
        }
        if (childrenOfLeaf.size() == 0) {
            // we shorten leaf titles here
            if (parentTreeNode.isDeep()) {
                leafNodeTitle = leafNodeTitle.replace(parentTreeNode.getOidValue() + ".", "");
            }
            if (parentTreeNode.isTableOrEntry()) {
                subTree = new TreeNode<>(new CatalogTableEntryItem(leafNodeTitle), oidValueOfLeaf, leafNodeTitle);
            } else {
                subTree = new TreeNode<>(new CatalogLeaf(leafNodeTitle), oidValueOfLeaf, leafNodeTitle);
            }
        }
        parentTreeNode.addChild(subTree);
        loadJsonTree(node, depth + 1, subTree);
    }

    /**
     * method to read json tree file to a jackson JsonNode object
     * @return
     */
    private JsonNode readJsonTree() {
        ObjectMapper om = new ObjectMapper();
        try {
            MibCatalogManager mcm = new MibCatalogManager(androidx.preference.PreferenceManager.getDefaultSharedPreferences(getActivity()));
            Reader is = new BufferedReader(
                    new InputStreamReader(mcm.getTreeFileInputStream(getContext()), StandardCharsets.UTF_8));
            JsonNode tree = om.readTree(is);
            is.close();
            return tree;
        } catch (IOException e) {
            Log.w(TAG, "error reading json tree: " + e.getMessage());
        }
        return null;
    }
    /* NOTE: the following code is for recycler tree view integration only !*/


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.catalog_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_import_mib) {

            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.mib_catalog_import_process))
                    .setMessage(getString(R.string.mib_catalog_import_dialog_description))
                    .setCancelable(true)
                    .setIcon(R.drawable.ic_info_black)
                    .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/zip");
                        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.mib_catalog_import_intent_extra));
                        startActivityForResult(intent, READ_REQUEST_CODE);
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "result received for MIB catalog .zip file import");

            Uri uri = null;
            if (data != null && data.getData() != null) {
                uri = data.getData();
                Log.i(TAG, "URI of import archive: " + uri.toString());

                MibCatalogArchiveManager fm = new MibCatalogArchiveManager(getActivity(), uri);
                MibCatalogManager mcm = new MibCatalogManager(androidx.preference.PreferenceManager.getDefaultSharedPreferences(getActivity()));

                if (mcm.isDuplicate(fm.getArchiveName())) {
                    Toast.makeText(getActivity(), R.string.mib_catalog_duplicate_toast_message, Toast.LENGTH_LONG).show();
                } else if (fm.isArchiveValid()) {
                    boolean success = fm.unpackZip();
                    if (success) {
                        Log.i(TAG, String.format("successfully imported '%s'", fm.getArchiveName()));

                        MibCatalog newCatalog = new MibCatalog(fm.getArchiveName());
                        mcm.getMibCatalog().add(newCatalog);
                        mcm.storeCatalog();
                        mcm.activateCatalog(fm.getArchiveName());

                        AsyncTask.execute(() -> OIDCatalog.getInstance(null, null).refresh());

                        Log.i(TAG, "added new MIB catalog and activated it");
                        Toast.makeText(getActivity(), String.format(getString(R.string.new_mib_catalog_created_toast_message),
                                fm.getArchiveName()), Toast.LENGTH_LONG).show();
                        // refresh this fragment
                        Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, newInstance())
                                .commit();
                    } else {
                        Log.w(TAG, String.format("Import of archive '%s' was not possible", fm.getArchiveName()));
                        Toast.makeText(getActivity(), getString(R.string.error_importing_mib_catalog_archive), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.w(TAG, String.format("archive '%s' is NOT valid!", fm.getArchiveName()));
                    Toast.makeText(getActivity(), R.string.invalid_mib_catalog_archive_toast_message, Toast.LENGTH_LONG).show();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
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
    public static class CatalogItem implements LayoutItemType {
        private String nodeName;

        public CatalogItem(String nodeName) {
            this.nodeName = nodeName;
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_node;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    public static class CatalogLeaf extends CatalogItem {
        public CatalogLeaf(String nodeName) {
            super(nodeName);
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_leaf;
        }
    }

    public static class CatalogTable extends CatalogItem {
        public CatalogTable(String nodeName) {
            super(nodeName);
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_table;
        }
    }

    public static class CatalogTableEntry extends CatalogItem {
        public CatalogTableEntry(String nodeName) {
            super(nodeName);
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_entry;
        }
    }

    public static class CatalogTableEntryItem extends CatalogItem {
        public CatalogTableEntryItem(String nodeName) {
            super(nodeName);
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_entry_leaf;
        }
    }

    /**
     * the following classes are used for recycler tree view
     */
    public class CatalogItemBinder extends TreeViewBinder<CatalogItemBinder.CatalogItemViewHolder> {
        @Override
        public CatalogItemViewHolder provideViewHolder(View itemView) {
            return new CatalogItemViewHolder(itemView);
        }

        @Override
        public void bindView(CatalogItemViewHolder holder, int position, TreeNode node) {
            CatalogItem fileNode = (CatalogItem) node.getContent();
            holder.getNodeInformationTextView().setText(fileNode.getNodeName());
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_leaf;
        }

        public class CatalogItemViewHolder extends TreeViewBinder.ViewHolder {
            private final TextView nodeInformationTextView;

            CatalogItemViewHolder(View rootView) {
                super(rootView);
                this.nodeInformationTextView = rootView.findViewById(R.id.query_catalog_list_item_text);
            }

            TextView getNodeInformationTextView() {
                return nodeInformationTextView;
            }
        }
    }

    public class DirectoryNodeBinder extends TreeViewBinder<DirectoryNodeBinder.DirectoryNodeViewHolder> {
        @Override
        public DirectoryNodeViewHolder provideViewHolder(View itemView) {
            return new DirectoryNodeViewHolder(itemView);
        }

        @Override
        public void bindView(DirectoryNodeViewHolder holder, int position, TreeNode node) {
            holder.ivArrow.setRotation(0);
            holder.ivArrow.setImageResource(R.drawable.ic_keyboard_arrow_right_black);
            int rotateDegree = node.isExpand() ? 90 : 0;
            holder.ivArrow.setRotation(rotateDegree);
            CatalogItem dirNode = (CatalogItem) node.getContent();
            holder.listItemText.setText(dirNode.getNodeName());
            if (node.isLeaf()) {
                holder.ivArrow.setVisibility(View.INVISIBLE);
            } else {
                holder.ivArrow.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_node;
        }

        public class DirectoryNodeViewHolder extends TreeViewBinder.ViewHolder {
            private ImageView ivArrow;
            private TextView listItemText;

            DirectoryNodeViewHolder(View rootView) {
                super(rootView);
                this.ivArrow = rootView.findViewById(R.id.iv_arrow);
                this.listItemText = rootView.findViewById(R.id.query_catalog_list_item_text);
            }

            ImageView getIvArrow() {
                return ivArrow;
            }

            public TextView getListItemText() {
                return listItemText;
            }
        }
    }

    public class TableNodeBinder extends DirectoryNodeBinder {
        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_table;
        }
    }

    public class TableEntryNodeBinder extends DirectoryNodeBinder {
        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_entry;
        }
    }

    public class TableEntryItemNodeBinder extends CatalogItemBinder {
        @Override
        public int getLayoutId() {
            return R.layout.catalog_item_entry_leaf;
        }
    }

    class TreeNodeListener implements TreeViewAdapter.OnTreeNodeListener {
        @Override
        public boolean onClick(TreeNode node, RecyclerView.ViewHolder holder) {
            if (!node.isLeaf()) {
                //Update and toggle the node.
                onToggle(!node.isExpand(), holder);
            }
            // only leafs are clickable directly
            if (node.isLeaf() && node.isQueryable()) {
                showOIDQueryDialog(node.getOidValue());
                return true;
            }
            return false;
        }

        @Override
        public boolean onLongTap(TreeNode node, RecyclerView.ViewHolder holder) {
            // only proper oid value nodes are allowed here
            if (node.isQueryable() && node.isDeep()) {
                showOIDQueryDialog(node.getOidValue());
                return true;
            }
            return false;
        }

        @Override
        public void onToggle(boolean isExpand, RecyclerView.ViewHolder holder) {
            if (holder instanceof DirectoryNodeBinder.DirectoryNodeViewHolder) {
                layout.scrollToPositionWithOffset(holder.getAdapterPosition(), 175);

                DirectoryNodeBinder.DirectoryNodeViewHolder dirDirectoryNodeViewHolder = (DirectoryNodeBinder.DirectoryNodeViewHolder) holder;
                final ImageView ivArrow = dirDirectoryNodeViewHolder.getIvArrow();
                if (ivArrow != null) {
                    int rotateDegree = isExpand ? 90 : -90;
                    ivArrow.animate().rotationBy(rotateDegree)
                            .start();
                }
            }
        }

        /**
         * helper method to show dialog to choose devices
         *
         * @param oidValue
         */
        private void showOIDQueryDialog(String oidValue) {
            if (getActivity() == null) {
                return;
            }
            showQueryTargetDialog(oidValue);
        }

        private void showQueryTargetDialog(final String oidValue) {
            new AlertHelper(getActivity()).showQueryTargetDialog(oidValue);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        layout = null;
    }
}
