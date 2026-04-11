package pl.zieleeksw.quizmi.attempt

import java.time.Instant

data class QuizAttemptDto(
    val id: Long,
    val courseId: Long,
    val quizId: Long,
    val userId: Long,
    val quizTitle: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val finishedAt: Instant
)
