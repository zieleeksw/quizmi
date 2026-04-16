package pl.zieleeksw.quizmi.course.domain

import org.springframework.data.jpa.repository.JpaRepository
import pl.zieleeksw.quizmi.course.CourseMembershipStatus
import java.util.Optional

interface CourseMembershipRepository : JpaRepository<CourseMembershipEntity, Long> {
    fun findByCourseIdAndUserId(courseId: Long, userId: Long): Optional<CourseMembershipEntity>

    fun findAllByUserIdAndCourseIdIn(userId: Long, courseIds: Collection<Long>): List<CourseMembershipEntity>

    fun findAllByCourseIdAndStatus(courseId: Long, status: CourseMembershipStatus): List<CourseMembershipEntity>

    fun countByCourseIdAndStatus(courseId: Long, status: CourseMembershipStatus): Long
}
