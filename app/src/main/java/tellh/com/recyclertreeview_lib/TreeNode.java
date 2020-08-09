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

package tellh.com.recyclertreeview_lib;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import org.emschu.snmp.cockpit.fragment.MibCatalogFragment;

/**
 * Created by tlh on 2016/10/1 :)
 */

public class TreeNode<T extends LayoutItemType> {
    private T content;
    private TreeNode<? extends LayoutItemType> parent;
    private List<TreeNode<? extends LayoutItemType>> childList;
    private boolean isExpand;
    private boolean isLocked;
    private String oidValue = null;
    private String nodeTitle = null;

    //the tree high
    private int height = UNDEFINE;

    private static final int UNDEFINE = -1;

    public TreeNode(@NonNull T content) {
        this.content = content;
        this.childList = new ArrayList<>();
    }

    public TreeNode(@NonNull T content, boolean isExpand) {
        this.content = content;
        this.childList = new ArrayList<>();
        this.isExpand = isExpand;
    }

    public TreeNode(@NonNull T content, @NonNull String oidValue, @NonNull String nodeTitle) {
        this.content = content;
        this.childList = new ArrayList<>();
        this.oidValue = oidValue;
        this.nodeTitle = nodeTitle;
    }

    public int getHeight() {
        if (isRoot())
            height = 0;
        else if (height == UNDEFINE)
            height = parent.getHeight() + 1;
        return height;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return childList == null || childList.isEmpty();
    }

    public boolean hasOID() {
        if (oidValue != null && !oidValue.isEmpty()) {
            return true;
        }
        return false;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    public List<TreeNode<? extends LayoutItemType>> getChildList() {
        return childList;
    }

    public void setChildList(List<TreeNode<? extends LayoutItemType>> childList) {
        this.childList.clear();
        for (TreeNode<? extends LayoutItemType> treeNode : childList) {
            addChild(treeNode);
        }
    }

    public TreeNode<? extends LayoutItemType> addChild(TreeNode<? extends LayoutItemType> node) {
        if (childList == null)
            childList = new ArrayList<>();
        childList.add(node);
        node.parent = this;
        return this;
    }

    public String getOidValue() {
        return oidValue;
    }

    public void setOidValue(String oidValue) {
        this.oidValue = oidValue;
    }

    public boolean toggle() {
        isExpand = !isExpand;
        return isExpand;
    }

    public void collapse() {
        if (isExpand) {
            isExpand = false;
        }
    }

    public void collapseAll() {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        for (TreeNode<? extends LayoutItemType> child : this.childList) {
            child.collapseAll();
        }
    }

    public void expand() {
        if (!isExpand) {
            isExpand = true;
        }
    }

    public void expandAll() {
        expand();
        if (childList == null || childList.isEmpty()) {
            return;
        }
        for (TreeNode<? extends LayoutItemType> child : this.childList) {
            child.expandAll();
        }
    }

    public boolean isExpand() {
        return isExpand;
    }

    public void setParent(TreeNode<? extends LayoutItemType> parent) {
        this.parent = parent;
    }

    public TreeNode<? extends LayoutItemType> getParent() {
        return parent;
    }

    public TreeNode<T> lock() {
        isLocked = true;
        return this;
    }

    public TreeNode<T> unlock() {
        isLocked = false;
        return this;
    }

    public boolean isLocked() {
        return isLocked;
    }

    @NonNull
    @Override
    public String toString() {
        return "TreeNode{" +
                "content=" + this.content +
                ", parent=" + (parent == null ? "null" : parent.getContent().toString()) +
                ", childList=" + (childList == null ? "null" : childList.toString()) +
                ", isExpand=" + isExpand +
                '}';
    }

    public String getNodeTitle() {
        return nodeTitle;
    }

    public void setNodeTitle(String nodeTitle) {
        this.nodeTitle = nodeTitle;
    }

    public boolean isQueryable() {
        if (!hasOID()) {
            return false;
        }
        if (null != getParent() && getParent().isTableOrEntry()) {
            return false;
        }
        return true;
    }

    public boolean isTableOrEntry() {
        if (!hasOID()) {
            return false;
        }
        if (getNodeTitle() != null) {
            return getNodeTitle().endsWith("Table") ||
                    getNodeTitle().endsWith("Entry");
        }
        return false;
    }

    public boolean isDeep() {
        int beforeSize = 0;
        if (getNodeTitle() == null) {
            beforeSize = 1;
            setNodeTitle(((MibCatalogFragment.CatalogItem) this.getContent()).getNodeName());
        } else {
            beforeSize = getNodeTitle().length();
        }
        // checks if it contains at least 3 dots
        return Math.abs(beforeSize - getNodeTitle().replace(".", "").length()) > 3;
    }
}
