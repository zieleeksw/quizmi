package pl.zieleeksw.quizmi.course.domain

import org.springframework.stereotype.Component

@Component
class CourseDescriptionValidator {

    fun validate(description: String?) {
        if (description == null || description.isBlank()) {
            throw IllegalArgumentException("Course description cannot be empty.")
        }

        val normalizedDescription = description.trim()

        if (normalizedDescription.length < MIN_LENGTH) {
            throw IllegalArgumentException("Course description is too short. Min length is 10 characters.")
        }

        if (normalizedDescription.length > MAX_LENGTH) {
            throw IllegalArgumentException("Course description is too long. Max length is 1000 characters.")
        }
    }

    companion object {
        private const val MIN_LENGTH = 10
        private const val MAX_LENGTH = 1000
    }
}
