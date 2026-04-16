package pl.zieleeksw.quizmi.course

import java.time.Instant

data class CourseMemberDto(
    val userId: Long,
    val email: String,
    val role: CourseMembershipRole,
    val status: CourseMembershipStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
