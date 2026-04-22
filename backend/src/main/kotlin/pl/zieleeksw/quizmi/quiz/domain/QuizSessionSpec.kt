package pl.zieleeksw.quizmi.quiz.domain

import pl.zieleeksw.quizmi.quiz.QuizMode
import pl.zieleeksw.quizmi.quiz.QuizOrderMode

data class QuizSessionSpec(
    val id: Long,
    val title: String,
    val mode: QuizMode,
    val randomCount: Int?,
    val questionOrder: QuizOrderMode,
    val answerOrder: QuizOrderMode,
    val questionIds: List<Long>,
    val categoryIds: List<Long>
)
