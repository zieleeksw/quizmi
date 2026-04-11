package pl.zieleeksw.quizmi.attempt.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface QuizAttemptRepository : JpaRepository<QuizAttemptEntity, Long> {
    fun findAllByCourseIdAndUserIdOrderByFinishedAtDesc(courseId: Long, userId: Long): List<QuizAttemptEntity>
    fun findByIdAndCourseIdAndUserId(attemptId: Long, courseId: Long, userId: Long): Optional<QuizAttemptEntity>
}
