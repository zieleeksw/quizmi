package pl.zieleeksw.quizmi.attempt

import java.time.Instant

data class QuizSessionDto(
    val id: Long,
    val courseId: Long,
    val quizId: Long,
    val userId: Long,
    val quizTitle: String,
    val questionIds: List<Long>,
    val answerOrderByQuestion: Map<Long, List<Long>>,
    val checkedQuestionIds: List<Long>,
    val currentIndex: Int,
    val furthestIndex: Int,
    val answers: Map<Long, List<Long>>,
    val updatedAt: Instant
)
