package pl.zieleeksw.quizmi.feedback

data class AiFeedbackRequest(
    val selectedAnswerIds: List<Long>?
)
