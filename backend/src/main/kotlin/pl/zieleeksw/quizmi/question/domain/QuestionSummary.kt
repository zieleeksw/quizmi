package pl.zieleeksw.quizmi.question.domain

data class QuestionSummary(
    val id: Long,
    val categoryIds: Set<Long>
)
