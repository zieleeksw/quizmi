package pl.zieleeksw.quizmi.feedback

data class AiFeedbackDto(
    val misunderstanding: String,
    val reasoning: String,
    val hint: String,
    val generatedByAi: Boolean
)
