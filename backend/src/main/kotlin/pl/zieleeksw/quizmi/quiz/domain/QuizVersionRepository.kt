package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface QuizVersionRepository : JpaRepository<QuizVersionEntity, Long> {
    fun findByQuizIdAndVersionNumber(quizId: Long, versionNumber: Int): Optional<QuizVersionEntity>
    fun findAllByQuizIdOrderByVersionNumberDesc(quizId: Long): List<QuizVersionEntity>

    @Query(
        value = """
            SELECT qv.*
            FROM quiz_versions qv
            JOIN quizzes q
              ON q.id = qv.quiz_id
             AND q.current_version_number = qv.version_number
            WHERE q.id IN (:quizIds)
        """,
        nativeQuery = true
    )
    fun findCurrentVersionsByQuizIds(@Param("quizIds") quizIds: Collection<Long>): List<QuizVersionEntity>
}
