package com.example.itemmanagementandroid.ui.screens.itemlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import com.example.itemmanagementandroid.ui.components.UriImage

@Composable
fun ItemListScreen(
    state: ItemListUiState,
    onNavigate: (AppRoute) -> Unit,
    onSearchKeywordChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onSortOptionChanged: (ItemListSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Item List Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Include deleted: ${state.includeDeleted} | Loaded items: ${state.items.size}",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemListScreenTestTags.SEARCH_INPUT),
            value = state.searchKeyword,
            onValueChange = onSearchKeywordChanged,
            singleLine = true,
            label = { Text(text = "Search Items") },
            placeholder = { Text(text = "Search name/description/place/tags") }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemListScreenTestTags.CATEGORY_FILTER_ROW),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    modifier = Modifier.testTag(ItemListScreenTestTags.CATEGORY_FILTER_ALL_BUTTON),
                    selected = state.selectedCategoryId == null,
                    onClick = { onCategoryFilterChanged(null) },
                    label = { Text(text = "All") }
                )
            }

            items(state.categoryFilters, key = { category -> category.id }) { category ->
                val archivedSuffix = if (category.isArchived) " (Archived)" else ""
                FilterChip(
                    modifier = Modifier.testTag(
                        ItemListScreenTestTags.categoryFilterButton(category.id)
                    ),
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onCategoryFilterChanged(category.id) },
                    label = { Text(text = "${category.name}$archivedSuffix") }
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemListScreenTestTags.SORT_ROW),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ItemListSortOption.entries, key = { option -> option.name }) { option ->
                FilterChip(
                    modifier = Modifier.testTag(ItemListScreenTestTags.sortOptionButton(option)),
                    selected = state.sortOption == option,
                    onClick = { onSortOptionChanged(option) },
                    label = { Text(text = option.label()) }
                )
            }
        }

        if (state.isLoading) {
            Text(text = "Loading items...", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }

        when {
            state.shouldShowEmptyState -> {
                Text(
                    modifier = Modifier.testTag(ItemListScreenTestTags.EMPTY_STATE_TEXT),
                    text = "No items yet. Add your first item.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.shouldShowNoResultsState -> {
                Text(
                    modifier = Modifier.testTag(ItemListScreenTestTags.NO_RESULTS_TEXT),
                    text = "No results for the selected filter.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag(ItemListScreenTestTags.ITEM_LIST),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { item -> item.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ItemListScreenTestTags.itemRow(item.id))
                                .clickable { onNavigate(AppRoute.ItemDetail(itemId = item.id)) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UriImage(
                                uri = state.coverUriByItemId[item.id],
                                contentDescription = "Item cover image",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .testTag(ItemListScreenTestTags.itemCover(item.id)),
                                placeholderText = "No Cover"
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Date: ${item.purchaseDate ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Price: ${item.purchasePrice?.toString() ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemListScreenTestTags.GO_TO_ITEM_DETAIL_BUTTON),
            onClick = { onNavigate(AppRoute.ItemDetail()) }
        ) {
            Text(text = "Go To Item Detail")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemListScreenTestTags.GO_TO_ITEM_EDIT_BUTTON),
            onClick = { onNavigate(AppRoute.ItemEdit()) }
        ) {
            Text(text = "New Item")
        }
    }
}

private fun ItemListSortOption.label(): String {
    return when (this) {
        ItemListSortOption.RECENTLY_ADDED -> "Recently Added"
        ItemListSortOption.RECENTLY_UPDATED -> "Recently Updated"
        ItemListSortOption.PURCHASE_DATE -> "Purchase Date"
        ItemListSortOption.PURCHASE_PRICE -> "Price"
    }
}

object ItemListScreenTestTags {
    const val SEARCH_INPUT = "item_list_search_input"
    const val CATEGORY_FILTER_ROW = "item_list_category_filter_row"
    const val CATEGORY_FILTER_ALL_BUTTON = "item_list_category_filter_all_button"
    const val SORT_ROW = "item_list_sort_row"
    const val ITEM_LIST = "item_list_items"
    const val EMPTY_STATE_TEXT = "item_list_empty_state_text"
    const val NO_RESULTS_TEXT = "item_list_no_results_text"
    const val GO_TO_ITEM_DETAIL_BUTTON = "item_list_go_to_item_detail_button"
    const val GO_TO_ITEM_EDIT_BUTTON = "item_list_go_to_item_edit_button"

    fun categoryFilterButton(categoryId: String): String = "item_list_filter_$categoryId"
    fun sortOptionButton(option: ItemListSortOption): String = "item_list_sort_${option.name}"
    fun itemRow(itemId: String): String = "item_list_row_$itemId"
    fun itemCover(itemId: String): String = "item_list_cover_$itemId"
}
