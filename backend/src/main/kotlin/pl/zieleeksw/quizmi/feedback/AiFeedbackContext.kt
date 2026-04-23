package pl.zieleeksw.quizmi.feedback

data class AiFeedbackContext(
    val prompt: String,
    val explanation: String?,
    val categories: List<String>,
    val answers: List<AiFeedbackAnswerContext>,
    val selectedAnswerIds: Set<Long>
)

data class AiFeedbackAnswerContext(
    val id: Long,
    val content: String,
    val correct: Boolean,
    val selected: Boolean
)
