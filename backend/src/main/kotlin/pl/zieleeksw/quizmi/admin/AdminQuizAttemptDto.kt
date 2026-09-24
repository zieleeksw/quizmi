package pl.zieleeksw.quizmi.admin

import java.time.Instant

data class AdminQuizAttemptDto(
    val attemptId: Long,
    val finishedAt: Instant,
    val courseId: Long,
    val courseName: String,
    val quizId: Long,
    val quizTitle: String,
    val userId: Long,
    val playerEmail: String,
    val correctAnswers: Int,
    val totalQuestions: Int
)
