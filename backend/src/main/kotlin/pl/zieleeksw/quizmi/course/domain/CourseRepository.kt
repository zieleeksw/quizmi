package pl.zieleeksw.quizmi.course.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<CourseEntity, Long> {
    fun findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId: Long): List<CourseEntity>

    fun findAllByOrderByCreatedAtDesc(): List<CourseEntity>
}
