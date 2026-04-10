package pl.zieleeksw.quizmi.question

data class QuestionPageDto(
    val items: List<QuestionDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
