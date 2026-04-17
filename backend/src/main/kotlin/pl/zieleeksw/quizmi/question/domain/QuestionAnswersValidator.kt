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
            val plainTextContent = extractPlainText(answer.content)

            if (plainTextContent.isBlank()) {
                throw IllegalArgumentException("Question answer content cannot be empty.")
            }

            if (answer.content.trim().length > MAX_LENGTH) {
                throw IllegalArgumentException("Question answer content is too long. Max length is $MAX_LENGTH characters.")
            }
        }

        val normalizedAnswers = answers
            .map { answer ->
                extractPlainText(answer.content).replace(Regex("\\s+"), " ").trim().lowercase()
            }

        if (normalizedAnswers.distinct().size != normalizedAnswers.size) {
            throw IllegalArgumentException("Question answers must be unique.")
        }
    }

    private fun extractPlainText(content: String?): String {
        if (content.isNullOrBlank()) {
            return ""
        }

        return content
            .replace(BREAK_TAG_REGEX, " ")
            .replace(HTML_TAG_REGEX, "")
            .replace("&nbsp;", " ")
            .trim()
    }

    companion object {
        private const val MAX_LENGTH = 1000
        private val BREAK_TAG_REGEX = Regex("(?i)<br\\s*/?>")
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
    }
}
