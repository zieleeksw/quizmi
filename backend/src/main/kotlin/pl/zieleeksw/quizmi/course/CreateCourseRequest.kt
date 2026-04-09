package pl.zieleeksw.quizmi.course

data class CreateCourseRequest(
    @field:ValidCourseName
    val name: String?,
    @field:ValidCourseDescription
    val description: String?
)
