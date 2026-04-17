package pl.zieleeksw.quizmi.question

import java.time.Instant

data class QuestionDto(
    val id: Long,
    val courseId: Long,
    val currentVersionNumber: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val prompt: String,
    val explanation: String?,
    val categories: List<QuestionCategoryDto>,
    val answers: List<QuestionAnswerDto>
)
