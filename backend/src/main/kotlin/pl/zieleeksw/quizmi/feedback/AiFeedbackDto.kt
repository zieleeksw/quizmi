package pl.zieleeksw.quizmi.feedback

data class AiFeedbackDto(
    val feedback: String,
    val generatedByAi: Boolean
)
