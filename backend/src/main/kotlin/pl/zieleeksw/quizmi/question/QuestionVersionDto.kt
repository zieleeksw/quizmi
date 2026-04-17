package pl.zieleeksw.quizmi.question

import java.time.Instant

data class QuestionVersionDto(
    val id: Long,
    val questionId: Long,
    val versionNumber: Int,
    val createdAt: Instant,
    val prompt: String,
    val explanation: String?,
    val categories: List<QuestionCategoryDto>,
    val answers: List<QuestionAnswerDto>
)
