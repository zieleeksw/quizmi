package pl.zieleeksw.quizmi.attempt

import java.time.Instant

data class QuizAttemptDetailDto(
    val id: Long,
    val courseId: Long,
    val quizId: Long,
    val userId: Long,
    val quizTitle: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val finishedAt: Instant,
    val questions: List<QuizAttemptQuestionReviewDto>
)
