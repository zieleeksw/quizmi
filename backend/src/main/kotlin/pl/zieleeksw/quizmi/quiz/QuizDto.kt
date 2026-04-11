package pl.zieleeksw.quizmi.quiz

import java.time.Instant

data class QuizDto(
    val id: Long,
    val courseId: Long,
    val active: Boolean,
    val currentVersionNumber: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val title: String,
    val mode: QuizMode,
    val randomCount: Int?,
    val questionOrder: QuizOrderMode,
    val answerOrder: QuizOrderMode,
    val questionIds: List<Long>,
    val categories: List<QuizCategoryDto>,
    val resolvedQuestionCount: Int
)
