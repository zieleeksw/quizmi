package pl.zieleeksw.quizmi.admin.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.zieleeksw.quizmi.admin.AdminQuizAttemptDto
import pl.zieleeksw.quizmi.attempt.domain.QuizAttemptEntity

interface AdminQuizAttemptRepository : JpaRepository<QuizAttemptEntity, Long> {

    @Query(
        """
        SELECT new pl.zieleeksw.quizmi.admin.AdminQuizAttemptDto(
            a.id, a.finishedAt, a.courseId, c.name, a.quizId, a.quizTitle, a.userId, u.email, a.correctAnswers, a.totalQuestions
        )
        FROM QuizAttemptEntity a, CourseEntity c, UserEntity u
        WHERE a.courseId = c.id AND a.userId = u.id
        ORDER BY a.finishedAt DESC
        """
    )
    fun findAllForAdminConsole(): List<AdminQuizAttemptDto>
}
