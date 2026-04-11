package pl.zieleeksw.quizmi.attempt.domain

class QuizSessionNotFoundException private constructor(
    message: String
) : RuntimeException(message) {

    companion object {
        fun forQuizId(quizId: Long): QuizSessionNotFoundException {
            return QuizSessionNotFoundException("Quiz session for quiz $quizId was not found.")
        }
    }
}
