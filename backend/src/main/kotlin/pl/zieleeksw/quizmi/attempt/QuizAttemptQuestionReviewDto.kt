package pl.zieleeksw.quizmi.attempt

data class QuizAttemptQuestionReviewDto(
    val questionId: Long,
    val prompt: String,
    val selectedAnswerIds: List<Long>,
    val correctAnswerIds: List<Long>,
    val answeredCorrectly: Boolean,
    val answers: List<QuizAttemptAnswerReviewDto>
)
