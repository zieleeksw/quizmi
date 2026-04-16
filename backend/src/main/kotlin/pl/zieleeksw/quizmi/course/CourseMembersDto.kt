package pl.zieleeksw.quizmi.course

data class CourseMembersDto(
    val members: List<CourseMemberDto>,
    val pendingRequests: List<CourseMemberDto>
)
