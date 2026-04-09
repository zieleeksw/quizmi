package pl.zieleeksw.quizmi.course

data class UpdateCourseRequest(
    @field:ValidCourseName
    val name: String?,
    @field:ValidCourseDescription
    val description: String?
)
