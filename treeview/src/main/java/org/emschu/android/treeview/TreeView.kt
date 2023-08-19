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

package org.emschu.android.treeview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * this enum represents a node type
 */
enum class TreeNodeType(
    val key: String,
    val icon: ImageVector,
) {
    NODE("node", Icons.Filled.Folder),
    LEAF("leaf", Icons.Filled.Description),
    TABLE("table", Icons.Filled.FormatListNumbered),
    TABLE_ENTRY("table_entry", Icons.Filled.List),
    TABLE_ENTRY_ITEM("table_entry_item", Icons.Filled.LinearScale),
}

/**
 * final class for a configuration option instance
 */
@Stable
data class TreeNodeConfiguration(
    val baseNodeIndent: Dp = 0.dp,
    val baseLeafIndent: Dp = 28.dp,
    val baseLevelIndent: Dp = 8.dp,
    val baseNodeAndLeafSpacing: Dp = 4.dp,
    val nodeVerticalPadding: Dp = 6.dp,
    val nodeHorizontalPadding: Dp = 8.dp,
    val toggleAnimationDurationMillis: Int = 150,
    val nodeIconTint: Color = Color.DarkGray,
    val nodeArrowTint: Color = Color.LightGray,
)

/**
 * this class should be used to model a tree
 */
@Stable
open class TreeNode(
    val parent: TreeNode?,
    val nodeLabel: String,
    val children: MutableList<TreeNode> = mutableListOf(),
    val treeNodeType: TreeNodeType = TreeNodeType.NODE,
    val nodeContent: String = "",
) {
    fun addChild(child: TreeNode) {
        this.children.add(child)
    }
}

/**
 * this is the "internal" data structure of the TreeView. Should not be used outside of this file except to consume the type
 */
data class TreeItemNodeFlat(
    val id: Int,
    val parentId: Int,
    val level: Int,
    val nodeLabel: String,
    val isLeaf: Boolean,
    val childrenIds: IntArray = intArrayOf(),
    val nodeContent: String = "",
    val contentType: TreeNodeType = TreeNodeType.NODE,
    var isExpanded: Boolean = false,
    var isVisible: Boolean = false,
) {
    fun isQueryable(): Boolean {
        if (nodeContent.isBlank()) {
            return false
        }
        if (-1 != parentId && contentType == TreeNodeType.TABLE_ENTRY_ITEM) {
            return false
        }
        return true
    }

    // this method is required because of the children id array property
    // ignore isVisible + isExpanded state
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TreeItemNodeFlat

        if (id != other.id) return false
        if (parentId != other.parentId) return false
        if (level != other.level) return false
        if (nodeLabel != other.nodeLabel) return false
        if (isLeaf != other.isLeaf) return false
        if (!childrenIds.contentEquals(other.childrenIds)) return false
        if (nodeContent != other.nodeContent) return false
        if (contentType != other.contentType) return false

        return true
    }

    // this method is required because of the children id array property
    // ignore isVisible + isExpanded state
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + parentId
        result = 31 * result + level
        result = 31 * result + nodeLabel.hashCode()
        result = 31 * result + isLeaf.hashCode()
        result = 31 * result + childrenIds.contentHashCode()
        result = 31 * result + nodeContent.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/**
 * the central component
 */
@ExperimentalFoundationApi
@Composable
fun TreeView(
    treeViewModel: TreeViewModel,
    listState: LazyListState = rememberLazyListState(),
    onClick: (TreeItemNodeFlat) -> Unit = {},
    onLongClick: (TreeItemNodeFlat) -> Unit = {},
    onDoubleClick: (TreeItemNodeFlat) -> Unit = {},
    treeNodeConfiguration: TreeNodeConfiguration = TreeNodeConfiguration(),
) {
    LazyColumn(state = listState, modifier = Modifier.verticalScrollBar(listState)) {
        itemsIndexed(treeViewModel.visibleMibCatalog, key = { _, item -> item.id }) { _, item ->
            DisplayNode(
                item,
                treeViewModel,
                onClick,
                onLongClick,
                onDoubleClick,
                treeNodeConfiguration,
            )
        }
    }
}

/**
 * recursive tree traversal to convert a given tree into a flat 1D list which is the input of the LazyColumn
 */
fun traverseTreeToList(
    tree: TreeNode,
    startLevel: Int,
    itemList: MutableList<TreeItemNodeFlat>,
    parentId: Int = -1,
    itemCounter: AtomicInteger = AtomicInteger(1), // should always start with "1"
): Int {
    // create root node
    val rootId = itemCounter.getAndIncrement()
    val childrenList = mutableListOf<Int>()
    tree.children.forEach {
        val childId = traverseTreeToList(it, startLevel + 1, itemList, rootId, itemCounter)
        childrenList.add(childId)
    }
    itemList.add(
        TreeItemNodeFlat(
            rootId,
            parentId,
            startLevel,
            tree.nodeLabel,
            tree.children.isEmpty(),
            childrenList.toIntArray(),
            contentType = tree.treeNodeType,
            nodeContent = tree.nodeContent
        )
    )
    return rootId
}

@ExperimentalFoundationApi
@Composable
private fun DisplayNode(
    treeItemNode: TreeItemNodeFlat,
    treeViewModel: TreeViewModel,
    onClick: (TreeItemNodeFlat) -> Unit = {},
    onLongClick: (TreeItemNodeFlat) -> Unit = {},
    onDoubleClick: (TreeItemNodeFlat) -> Unit = {},
    treeNodeConfiguration: TreeNodeConfiguration,
) {
    if (!treeItemNode.isExpanded && !treeItemNode.isVisible) {
        // do not display nodes which are not expanded
        return
    }
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    // node expansion/folding logic
                    scope.launch {
                        treeViewModel.toggleState(treeItemNode)
                        treeViewModel.updateList()
                        // also hide children
                        onClick(treeItemNode)
                        treeViewModel.updateLastId(treeItemNode.id)
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        onDoubleClick(treeItemNode)
                        treeViewModel.updateLastId(treeItemNode.id)
                    }
                },
                onLongClick = {
                    scope.launch {
                        onLongClick(treeItemNode)
                        treeViewModel.updateLastId(treeItemNode.id)
                    }
                },
            )
            .fillMaxWidth()
            .padding(
                horizontal = treeNodeConfiguration.nodeHorizontalPadding,
                vertical = treeNodeConfiguration.nodeVerticalPadding
            )
    ) {
        if (treeItemNode.isLeaf) {
            ShowLeaf(treeItemNode, treeNodeConfiguration)
        } else {
            ShowNode(treeItemNode, treeNodeConfiguration)
        }
    }
}

/**
 * Composable to show a non-leaf node with icons etc.
 */
@Composable
private fun ShowNode(
    item: TreeItemNodeFlat, treeNodeConfiguration: TreeNodeConfiguration
) {
    if (item.id != 1) {
        // ignore icon + spacing for root node
        Spacer(Modifier.width((treeNodeConfiguration.baseNodeIndent + treeNodeConfiguration.baseLevelIndent * (item.level - 1))))
        val angle: Float by animateFloatAsState(
            targetValue = if (!item.isLeaf && item.isExpanded) {
                45f
            } else {
                0f
            }, animationSpec = tween(treeNodeConfiguration.toggleAnimationDurationMillis, 20, LinearEasing)
        )
        Icon(
            Icons.Outlined.ArrowRight,
            contentDescription = "Arrow Right",
            Modifier.rotate(angle),
            tint = treeNodeConfiguration.nodeArrowTint
        )
    }
    Icon(item.contentType.icon, "node icon", tint = treeNodeConfiguration.nodeIconTint)
    Spacer(modifier = Modifier.width(treeNodeConfiguration.baseNodeAndLeafSpacing))
    TreeNodeText(
        text = item.nodeLabel, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.body1.fontSize
    )
}

/**
 * Composable to show a leaf node with icons etc.
 */
@Composable
private fun ShowLeaf(item: TreeItemNodeFlat, treeNodeConfiguration: TreeNodeConfiguration) {
    Spacer(Modifier.width(treeNodeConfiguration.baseLeafIndent + treeNodeConfiguration.baseLevelIndent * (item.level - 1)))
    Icon(item.contentType.icon, "node icon", tint = treeNodeConfiguration.nodeIconTint)
    Spacer(modifier = Modifier.width(treeNodeConfiguration.baseNodeAndLeafSpacing))
    TreeNodeText(text = item.nodeLabel)
}

@Composable
private fun TreeNodeText(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = MaterialTheme.typography.body2.fontSize
) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
            text,
            softWrap = true,
            overflow = TextOverflow.Visible,
            maxLines = 5,
            fontWeight = fontWeight,
            fontSize = fontSize,
        )
    }
}