package com.example.itemmanagementandroid.domain.usecase.category

import com.example.itemmanagementandroid.domain.repository.CategoryRepository

class ReorderCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(categoryIds: List<String>) {
        categoryRepository.reorder(categoryIds = categoryIds)
    }
}
