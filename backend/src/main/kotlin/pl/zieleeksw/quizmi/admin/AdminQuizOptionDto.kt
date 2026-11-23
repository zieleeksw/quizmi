package pl.zieleeksw.quizmi.admin

data class AdminQuizOptionDto(
    val quizId: Long,
    val quizTitle: String,
    val courseId: Long,
    val courseName: String
)
