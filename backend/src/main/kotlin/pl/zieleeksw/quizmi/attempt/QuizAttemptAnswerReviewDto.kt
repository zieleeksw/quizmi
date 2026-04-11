package pl.zieleeksw.quizmi.attempt

data class QuizAttemptAnswerReviewDto(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val correct: Boolean
)
