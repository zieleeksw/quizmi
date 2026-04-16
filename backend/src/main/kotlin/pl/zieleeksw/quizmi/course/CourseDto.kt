package pl.zieleeksw.quizmi.course

import java.time.Instant

data class CourseDto(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Instant,
    val ownerUserId: Long,
    val ownerEmail: String,
    val membershipRole: CourseMembershipRole?,
    val membershipStatus: CourseMembershipStatus?,
    val canAccess: Boolean,
    val canManage: Boolean,
    val pendingRequestsCount: Int
)
