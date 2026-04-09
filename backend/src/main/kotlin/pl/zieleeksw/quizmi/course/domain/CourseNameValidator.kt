package pl.zieleeksw.quizmi.course.domain

import org.springframework.stereotype.Component

@Component
class CourseNameValidator {

    fun validate(name: String?) {
        if (name == null || name.isBlank()) {
            throw IllegalArgumentException("Course name cannot be empty.")
        }

        val normalizedName = name.trim()

        if (normalizedName.length < MIN_LENGTH) {
            throw IllegalArgumentException("Course name is too short. Min length is 3 characters.")
        }

        if (normalizedName.length > MAX_LENGTH) {
            throw IllegalArgumentException("Course name is too long. Max length is 120 characters.")
        }
    }

    companion object {
        private const val MIN_LENGTH = 3
        private const val MAX_LENGTH = 120
    }
}
