package com.example.itemmanagementandroid.domain.usecase.category

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.repository.CategoryRepository

class SetCategoryArchivedUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        categoryId: String,
        archived: Boolean
    ): Category {
        return categoryRepository.setArchived(
            categoryId = categoryId,
            archived = archived
        )
    }
}
