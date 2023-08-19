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

package org.emschu.snmp.cockpit

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.emschu.android.treeview.TreeItemNodeFlat
import org.emschu.android.treeview.TreeNode
import org.emschu.android.treeview.traverseTreeToList
import org.emschu.snmp.cockpit.snmp.MibCatalogLoader
import org.emschu.snmp.cockpit.ui.screens.MIBCatalogView
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MibCatalogTest : AbstractCockpitAppTest() {
    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    @OptIn(ExperimentalMaterialApi::class)
    @Before
    fun setupMibCatalogContent() {
        composeTestRule.setContent {
            MIBCatalogView(
                mainViewModel = mainViewModel,
                bottomSheetScaffoldState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
                treeViewModel = treeViewModel,
            )
        }
        composeTestRule.onRoot().onChildren().assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription("node icon").assertCountEquals(0)
    }

    @Test
    fun testSimpleCatalogWith0Item() {
        val itemList = mutableListOf<TreeItemNodeFlat>()
        treeViewModel.loadList(itemList)
        composeTestRule.onAllNodesWithContentDescription("node icon").assertCountEquals(0)
    }

    @Test
    fun testSimpleCatalogWith1Item() {
        val itemList = mutableListOf<TreeItemNodeFlat>()
        val tree = TreeNode(null, "1")
        traverseTreeToList(tree, 0, itemList)
        treeViewModel.loadList(itemList)
        composeTestRule.onAllNodesWithContentDescription("node icon").assertCountEquals(1)
    }

    @Test
    fun testSimpleCatalogWith2Items() {
        val itemList = mutableListOf<TreeItemNodeFlat>()
        val tree = TreeNode(null, "1")
        val child1 = TreeNode(tree, "Child 1")
        tree.addChild(child1)
        traverseTreeToList(tree, 0, itemList)
        treeViewModel.loadList(itemList)

        composeTestRule.onAllNodes(hasContentDescription("node icon")).assertCountEquals(2)
    }

    @Test
    fun testSimpleCatalogWith3Items() {
        val itemList = mutableListOf<TreeItemNodeFlat>()
        val tree = TreeNode(null, "1")
        val child1 = TreeNode(tree, "Child 1")
        child1.addChild(TreeNode(child1, "Child 2"))
        tree.addChild(child1)
        traverseTreeToList(tree, 0, itemList)
        treeViewModel.loadList(itemList)

        composeTestRule.onAllNodes(hasContentDescription("node icon")).assertCountEquals(3)
    }

    @Test
    fun testSimpleCatalogWithDefaultMibCatalogItems() {
        val itemList = mutableListOf<TreeItemNodeFlat>()

        val tree = TreeNode(null, "1")
        val mibCatalogLoader = MibCatalogLoader()
        mibCatalogLoader.loadMibTree(appContext, tree)

        traverseTreeToList(tree, 0, itemList)
        treeViewModel.loadList(itemList)

        // check visible nodes in default catalog
        composeTestRule.onAllNodes(hasContentDescription("node icon")).assertCountEquals(12)
    }
}