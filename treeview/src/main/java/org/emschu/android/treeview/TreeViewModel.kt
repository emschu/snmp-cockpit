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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * this class holds view state of the TreeView and is used by it.
 */
class TreeViewModel(val state: SavedStateHandle) : ViewModel() {
    private val _mibCatalog = mutableStateListOf<TreeItemNodeFlat>()
    private val _lastId = mutableStateOf(0)

    val visibleMibCatalog = mutableStateListOf<TreeItemNodeFlat>()

    val lastId: State<Int>
        get() = _lastId

    fun loadList(itemList: List<TreeItemNodeFlat>) {
        viewModelScope.launch {
            this@TreeViewModel._mibCatalog.clear()
            this@TreeViewModel._mibCatalog.addAll(itemList)
            this@TreeViewModel._mibCatalog.sortBy { it.id }

            // "auto-expand"
            if (this@TreeViewModel._mibCatalog.size > 0) {
                toggleState(this@TreeViewModel._mibCatalog[0])
            }
            if (this@TreeViewModel._mibCatalog.size > 3) {
                toggleState(this@TreeViewModel._mibCatalog[3])
            }
            updateList()
        }
    }

    /**
     * updateList() should called afterwards
     */
    fun toggleState(treeNodeItem: TreeItemNodeFlat) {
        viewModelScope.launch {
            // cannot collapse root node
            if (treeNodeItem.isExpanded && treeNodeItem.isVisible && treeNodeItem.parentId != -1) {
                // close
                if (treeNodeItem.isLeaf) {
                    // not closable
                } else {
                    treeNodeItem.isVisible = true
                    treeNodeItem.isExpanded = false

                    // recursively collapse
                    collapseChildren(treeNodeItem)
                }
            } else {
                // open it + children
                treeNodeItem.isExpanded = true
                treeNodeItem.isVisible = true
                treeNodeItem.childrenIds.forEach {
                    val child = _mibCatalog[it - 1]
                    child.isExpanded = false
                    child.isVisible = true
                    if (child.childrenIds.size == 1) {
                        // automatically open child node if its only one
                        toggleState(child)
                    }
                }
            }
        }
    }

    private fun collapseChildren(treeNodeItem: TreeItemNodeFlat) {
        treeNodeItem.childrenIds.forEach {
            if (_mibCatalog[it - 1].isExpanded || _mibCatalog[it - 1].isVisible) {
                collapseChildren(_mibCatalog[it - 1])
            }
            _mibCatalog[it - 1].isExpanded = false
            _mibCatalog[it - 1].isVisible = false
        }
    }

    fun updateList() {
        visibleMibCatalog.clear()
        visibleMibCatalog.addAll(_mibCatalog.filter { it.isExpanded || it.isVisible })
    }

    fun updateLastId(lastId: Int) {
        this._lastId.value = lastId
    }
}