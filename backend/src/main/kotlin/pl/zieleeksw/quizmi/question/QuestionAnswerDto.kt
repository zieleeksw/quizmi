package pl.zieleeksw.quizmi.question

data class QuestionAnswerDto(
    val id: Long,
    val displayOrder: Int,
    val content: String,
    val correct: Boolean
)
