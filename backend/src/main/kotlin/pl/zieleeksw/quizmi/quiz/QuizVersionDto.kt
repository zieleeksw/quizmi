package pl.zieleeksw.quizmi.quiz

import java.time.Instant

data class QuizVersionDto(
    val id: Long,
    val quizId: Long,
    val versionNumber: Int,
    val createdAt: Instant,
    val title: String,
    val mode: QuizMode,
    val randomCount: Int?,
    val questionOrder: QuizOrderMode,
    val answerOrder: QuizOrderMode,
    val questionIds: List<Long>,
    val categories: List<QuizCategoryDto>,
    val resolvedQuestionCount: Int
)
