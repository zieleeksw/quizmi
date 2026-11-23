package pl.zieleeksw.quizmi.admin.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.zieleeksw.quizmi.admin.AdminCourseOptionDto
import pl.zieleeksw.quizmi.admin.AdminQuizAttemptDto
import pl.zieleeksw.quizmi.admin.AdminQuizOptionDto
import pl.zieleeksw.quizmi.attempt.domain.QuizAttemptEntity

interface AdminQuizAttemptRepository : JpaRepository<QuizAttemptEntity, Long> {

    @Query(
        value = """
            SELECT new pl.zieleeksw.quizmi.admin.AdminQuizAttemptDto(
                a.id, a.finishedAt, a.courseId, c.name, a.quizId, a.quizTitle, a.userId, u.email, a.correctAnswers, a.totalQuestions
            )
            FROM QuizAttemptEntity a, CourseEntity c, UserEntity u
            WHERE a.courseId = c.id AND a.userId = u.id
              AND (:courseId IS NULL OR a.courseId = :courseId)
              AND (:quizId IS NULL OR a.quizId = :quizId)
              AND (:userId IS NULL OR a.userId = :userId)
            ORDER BY a.finishedAt DESC
        """,
        countQuery = """
            SELECT count(a)
            FROM QuizAttemptEntity a, CourseEntity c, UserEntity u
            WHERE a.courseId = c.id AND a.userId = u.id
              AND (:courseId IS NULL OR a.courseId = :courseId)
              AND (:quizId IS NULL OR a.quizId = :quizId)
              AND (:userId IS NULL OR a.userId = :userId)
        """
    )
    fun findAllForAdminConsole(
        @Param("courseId") courseId: Long?,
        @Param("quizId") quizId: Long?,
        @Param("userId") userId: Long?,
        pageable: Pageable
    ): Page<AdminQuizAttemptDto>

    @Query(
        """
        SELECT DISTINCT new pl.zieleeksw.quizmi.admin.AdminCourseOptionDto(c.id, c.name)
        FROM QuizAttemptEntity a, CourseEntity c
        WHERE a.courseId = c.id
          AND (:userId IS NULL OR a.userId = :userId)
        ORDER BY c.name ASC
        """
    )
    fun findDistinctCourseOptions(@Param("userId") userId: Long?): List<AdminCourseOptionDto>

    @Query(
        """
        SELECT DISTINCT new pl.zieleeksw.quizmi.admin.AdminQuizOptionDto(a.quizId, a.quizTitle, c.id, c.name)
        FROM QuizAttemptEntity a, CourseEntity c
        WHERE a.courseId = c.id
          AND (:userId IS NULL OR a.userId = :userId)
          AND (:courseId IS NULL OR a.courseId = :courseId)
        ORDER BY a.quizTitle ASC
        """
    )
    fun findDistinctQuizOptions(
        @Param("userId") userId: Long?,
        @Param("courseId") courseId: Long?
    ): List<AdminQuizOptionDto>
}
