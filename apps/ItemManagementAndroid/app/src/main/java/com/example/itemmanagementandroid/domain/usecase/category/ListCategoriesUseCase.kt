package com.example.itemmanagementandroid.domain.usecase.category

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.repository.CategoryRepository

class ListCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(includeArchived: Boolean = false): List<Category> {
        return categoryRepository.list(includeArchived = includeArchived)
    }
}
