package pl.zieleeksw.quizmi.admin

data class AdminQuizAttemptPageDto(
    val items: List<AdminQuizAttemptDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
