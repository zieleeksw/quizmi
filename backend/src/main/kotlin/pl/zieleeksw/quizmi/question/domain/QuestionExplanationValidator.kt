package pl.zieleeksw.quizmi.question.domain

import org.springframework.stereotype.Component

@Component
class QuestionExplanationValidator {

    fun validate(explanation: String?) {
        if (explanation.isNullOrBlank()) {
            return
        }

        val normalizedExplanation = explanation.trim()

        if (normalizedExplanation.length > MAX_LENGTH) {
            throw IllegalArgumentException("Question explanation is too long. Max length is 2000 characters.")
        }
    }

    companion object {
        private const val MAX_LENGTH = 2000
    }
}
