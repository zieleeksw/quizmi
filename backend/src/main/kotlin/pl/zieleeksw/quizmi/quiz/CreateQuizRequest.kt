package pl.zieleeksw.quizmi.quiz

data class CreateQuizRequest(
    val title: String?,
    val mode: QuizMode?,
    val randomCount: Int?,
    val questionOrder: QuizOrderMode?,
    val answerOrder: QuizOrderMode?,
    val questionIds: List<Long>?,
    val categoryIds: List<Long>?
)
