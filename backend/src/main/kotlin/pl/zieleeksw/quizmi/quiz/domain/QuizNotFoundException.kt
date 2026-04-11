package pl.zieleeksw.quizmi.quiz.domain

class QuizNotFoundException private constructor(
    message: String
) : RuntimeException(message) {

    companion object {
        fun forId(id: Long): QuizNotFoundException {
            return QuizNotFoundException("Quiz with id $id was not found.")
        }
    }
}
