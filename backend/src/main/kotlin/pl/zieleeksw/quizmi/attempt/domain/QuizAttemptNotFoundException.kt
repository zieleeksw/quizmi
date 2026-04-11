package pl.zieleeksw.quizmi.attempt.domain

class QuizAttemptNotFoundException private constructor(
    message: String
) : RuntimeException(message) {

    companion object {
        fun forId(id: Long): QuizAttemptNotFoundException {
            return QuizAttemptNotFoundException("Quiz attempt with id $id was not found.")
        }
    }
}
