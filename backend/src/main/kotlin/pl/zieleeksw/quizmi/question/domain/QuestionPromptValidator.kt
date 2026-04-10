package pl.zieleeksw.quizmi.question.domain

import org.springframework.stereotype.Component

@Component
class QuestionPromptValidator {

    fun validate(prompt: String?) {
        if (prompt.isNullOrBlank()) {
            throw IllegalArgumentException("Question prompt cannot be empty.")
        }

        if (prompt.length < 12) {
            throw IllegalArgumentException("Question prompt is too short. Min length is 12 characters.")
        }

        if (prompt.length > 1000) {
            throw IllegalArgumentException("Question prompt is too long. Max length is 1000 characters.")
        }
    }
}
