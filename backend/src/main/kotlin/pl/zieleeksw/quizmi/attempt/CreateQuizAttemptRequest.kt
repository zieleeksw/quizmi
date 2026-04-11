package pl.zieleeksw.quizmi.attempt

data class CreateQuizAttemptRequest(
    val answers: List<QuizAttemptAnswerRequest>?
)
