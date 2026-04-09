package pl.zieleeksw.quizmi.category.domain

import org.springframework.stereotype.Component

@Component
class CategoryNameValidator {

    fun validate(name: String?) {
        if (name == null || name.isBlank()) {
            throw IllegalArgumentException("Category name cannot be empty.")
        }

        val normalizedName = name.trim()

        if (normalizedName.length < MIN_LENGTH) {
            throw IllegalArgumentException("Category name is too short. Min length is 2 characters.")
        }

        if (normalizedName.length > MAX_LENGTH) {
            throw IllegalArgumentException("Category name is too long. Max length is 120 characters.")
        }
    }

    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 120
    }
}
