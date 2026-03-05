package com.example.itemmanagementandroid.domain.usecase.category

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.repository.CategoryRepository

class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        categoryId: String,
        name: String
    ): Category {
        return categoryRepository.update(
            categoryId = categoryId,
            name = name
        )
    }
}
