package pl.zieleeksw.quizmi.attempt

data class QuizAttemptAnswerRequest(
    val questionId: Long?,
    val answerIds: List<Long>?
)
