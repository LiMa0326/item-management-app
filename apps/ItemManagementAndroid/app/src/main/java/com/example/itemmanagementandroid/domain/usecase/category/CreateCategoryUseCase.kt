package com.example.itemmanagementandroid.domain.usecase.category

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.repository.CategoryRepository

class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String): Category {
        return categoryRepository.create(name = name)
    }
}
