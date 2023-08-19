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

package org.emschu.snmp.cockpit.snmp

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.emschu.android.treeview.TreeNode
import java.io.IOException
import kotlin.math.abs

/**
 * this class loads the current MibCatalog from the json file to a tree object which is
 * populated by [loadMibTree] into `rootTreeNode`
 */
class MibCatalogLoader {
    fun loadMibTree(context: Context, rootTreeNode: TreeNode) {
        val parentNode: JsonNode? = readJsonTree(context)
        if (parentNode != null) {
            loadJsonTree(parentNode, 0, rootTreeNode)
        }
    }

    /**
     * method to read json tree file to a jackson JsonNode object
     *
     * @return
     */
    private fun readJsonTree(context: Context): JsonNode? {
        val om = ObjectMapper()
        try {
            val mcm = OIDCatalog.mibCatalogManager
            var tree: JsonNode?
            mcm.getTreeFileInputStream(context)
                .buffered()
                .use {
                    tree = om.readTree(it)
                }
            return tree
        } catch (e: IOException) {
            Log.w(
                "readJsonTree()", "error reading json tree: " + e.message
            )
        }
        return null
    }

    /**
     * heart of the tree
     *
     * @param parentNode
     * @param depth
     * @param parentTreeNode
     */
    private fun loadJsonTree(parentNode: JsonNode, depth: Int, parentTreeNode: TreeNode) {
        check(parentNode.isObject) { "invalid input given. no json object in $parentNode" }
        val childrenNodes = parentNode.path("children")
        check(
            !(childrenNodes == null || !childrenNodes.isObject)
        ) { "invalid children object received in json oid tree" }
        val childrenIterator: Iterator<JsonNode> = childrenNodes.iterator()
        val fieldNames = childrenNodes.fieldNames()
        while (childrenIterator.hasNext()) {
            val node = childrenIterator.next()
            val fieldName = fieldNames.next()
            // affects "children" fields
            val isLeaf = node.path("isLeaf")
                .asBoolean()
            if (isLeaf) {
                processLeaf(depth, parentTreeNode, childrenNodes, node)
            } else {
                if (fieldName == "." || fieldName == "1") {
                    loadJsonTree(node, depth, parentTreeNode)
                } else {
                    val subTree = TreeNode(parentTreeNode, fieldName)
                    parentTreeNode.addChild(subTree)
                    loadJsonTree(node, depth + 1, subTree)
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
    private fun processLeaf(
        depth: Int, parentTreeNode: TreeNode?, childrenNodes: JsonNode, node: JsonNode,
    ) {
        checkNotNull(parentTreeNode) { "invalid parent tree node" }
        val nodeAsnName = node.path("name")
            .textValue()
        val oidValueOfLeaf = node.path("oidValue")
            .textValue()
        var leafNodeTitle = "$oidValueOfLeaf - $nodeAsnName"
        var subTree: TreeNode
        subTree = if (nodeAsnName.endsWith("Table")) {
            TreeNode(parentTreeNode, leafNodeTitle, nodeContent = oidValueOfLeaf)
        } else if (nodeAsnName.endsWith("Entry")) {
            TreeNode(
                parentTreeNode,
                leafNodeTitle,
                nodeContent = oidValueOfLeaf,
                treeNodeType = org.emschu.android.treeview.TreeNodeType.TABLE_ENTRY
            )
        } else {
            TreeNode(parentTreeNode, leafNodeTitle, nodeContent = oidValueOfLeaf)
        }
        val childrenOfLeaf = node.path("children")
        check(childrenNodes.isObject) {
            Log.w(MibCatalogLoader::class.simpleName, "invalid children object received in json oid tree")
        }
        if (childrenOfLeaf.size() == 0) {
            // we shorten leaf titles here
            if (isTreeNodeDeep(parentTreeNode.nodeLabel)) {
                leafNodeTitle = leafNodeTitle.replace(parentTreeNode.nodeContent + ".", "")
            }

            subTree = if (parentTreeNode.nodeLabel.isNotBlank() && parentTreeNode.nodeLabel.endsWith("Table")) {
                TreeNode(
                    parentTreeNode,
                    leafNodeTitle,
                    nodeContent = oidValueOfLeaf,
                    treeNodeType = org.emschu.android.treeview.TreeNodeType.TABLE
                )
            } else if (parentTreeNode.nodeLabel.isNotBlank() && parentTreeNode.nodeLabel.endsWith("Entry")) {
                TreeNode(
                    parentTreeNode,
                    leafNodeTitle,
                    nodeContent = oidValueOfLeaf,
                    treeNodeType = org.emschu.android.treeview.TreeNodeType.TABLE_ENTRY_ITEM
                )
            } else {
                TreeNode(
                    parentTreeNode,
                    leafNodeTitle,
                    nodeContent = oidValueOfLeaf,
                    treeNodeType = org.emschu.android.treeview.TreeNodeType.LEAF
                )
            }
        }
        parentTreeNode.addChild(subTree)

        loadJsonTree(node, depth + 1, subTree)
    }

    companion object {
        /**
         * helper function specifically for an oid tree where the input is an OID string like "1.1.2.4.7.8."
         */
        fun isTreeNodeDeep(treeNodeLabel: String): Boolean {
            val beforeSize = if (treeNodeLabel.isBlank()) {
                1
            } else {
                treeNodeLabel.length
            }
            // checks if it contains at least 3 dots
            return abs(beforeSize - treeNodeLabel.replace(".", "").length) > 3
        }
    }
}