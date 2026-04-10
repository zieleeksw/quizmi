package pl.zieleeksw.quizmi.question.domain

import org.springframework.stereotype.Component

@Component
class QuestionCategoryIdsValidator {

    fun validate(categoryIds: List<Long>?) {
        if (categoryIds.isNullOrEmpty()) {
            throw IllegalArgumentException("Question must contain at least 1 category.")
        }

        if (categoryIds.any { it <= 0 }) {
            throw IllegalArgumentException("Question category ids are invalid.")
        }

        if (categoryIds.toSet().size != categoryIds.size) {
            throw IllegalArgumentException("Question category ids cannot contain duplicates.")
        }
    }
}
