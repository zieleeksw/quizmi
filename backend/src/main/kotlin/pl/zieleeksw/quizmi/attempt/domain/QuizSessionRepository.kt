package pl.zieleeksw.quizmi.attempt.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface QuizSessionRepository : JpaRepository<QuizSessionEntity, Long> {
    fun findAllByCourseIdAndUserIdOrderByUpdatedAtDesc(courseId: Long, userId: Long): List<QuizSessionEntity>
    fun findByCourseIdAndQuizIdAndUserId(courseId: Long, quizId: Long, userId: Long): Optional<QuizSessionEntity>
}
