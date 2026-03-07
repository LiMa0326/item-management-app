package com.example.itemmanagementandroid.ui.navigation

sealed interface AppRoute {
    val title: String

    data object Home : AppRoute {
        override val title: String = "Home"
    }

    data object Category : AppRoute {
        override val title: String = "Category"
    }

    data object ItemList : AppRoute {
        override val title: String = "Item List"
    }

    data class ItemDetail(val itemId: String? = null) : AppRoute {
        override val title: String = "Item Detail"
    }

    data class ItemEdit(val itemId: String? = null) : AppRoute {
        override val title: String = "Item Edit"
    }

    data object Settings : AppRoute {
        override val title: String = "Settings"
    }
}
