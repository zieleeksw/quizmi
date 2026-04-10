package pl.zieleeksw.quizmi.question.domain

class QuestionNotFoundException private constructor(
    override val message: String
) : RuntimeException(message) {
    companion object {
        fun forId(id: Long): QuestionNotFoundException {
            return QuestionNotFoundException("Question with id=$id was not found.")
        }
    }
}
