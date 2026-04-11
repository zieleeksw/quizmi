package pl.zieleeksw.quizmi.attempt

data class QuizSessionUpdateRequest(
    val currentIndex: Int?,
    val answers: List<QuizAttemptAnswerRequest>?
)
