package pl.zieleeksw.quizmi.question.domain

import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.question.QuestionAnswerRequest

@Component
class QuestionAnswersValidator {

    fun validate(answers: List<QuestionAnswerRequest>?) {
        if (answers.isNullOrEmpty() || answers.size < 2) {
            throw IllegalArgumentException("Question must contain at least 2 answers.")
        }

        if (answers.size > 6) {
            throw IllegalArgumentException("Question must contain at most 6 answers.")
        }

        if (answers.count { it.correct == true } < 1) {
            throw IllegalArgumentException("Question must contain at least 1 correct answer.")
        }

        answers.forEach { answer ->
            if (answer.content.isNullOrBlank()) {
                throw IllegalArgumentException("Question answer content cannot be empty.")
            }

            if (answer.content.trim().length > 300) {
                throw IllegalArgumentException("Question answer content is too long. Max length is 300 characters.")
            }
        }

        val normalizedAnswers = answers
            .map { answer ->
                answer.content?.trim()?.lowercase()
                    ?: throw IllegalArgumentException("Question answer content cannot be empty.")
            }

        if (normalizedAnswers.distinct().size != normalizedAnswers.size) {
            throw IllegalArgumentException("Question answers must be unique.")
        }
    }
}
