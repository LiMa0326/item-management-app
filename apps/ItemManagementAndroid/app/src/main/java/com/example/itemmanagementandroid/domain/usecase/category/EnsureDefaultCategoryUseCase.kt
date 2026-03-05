package com.example.itemmanagementandroid.domain.usecase.category

import com.example.itemmanagementandroid.domain.repository.CategoryRepository

class EnsureDefaultCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(): Boolean {
        return categoryRepository.ensureDefaultCategory()
    }
}
