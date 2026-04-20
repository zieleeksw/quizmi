package pl.zieleeksw.quizmi.question.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface QuestionVersionRepository : JpaRepository<QuestionVersionEntity, Long> {
    fun findAllByQuestionIdOrderByVersionNumberDesc(questionId: Long): List<QuestionVersionEntity>
    fun findByQuestionIdAndVersionNumber(questionId: Long, versionNumber: Int): Optional<QuestionVersionEntity>

    @Query(
        value = """
            SELECT qv.*
            FROM question_versions qv
            JOIN questions q
              ON q.id = qv.question_id
             AND q.current_version_number = qv.version_number
            WHERE q.id IN (:questionIds)
        """,
        nativeQuery = true
    )
    fun findCurrentVersionsByQuestionIds(@Param("questionIds") questionIds: Collection<Long>): List<QuestionVersionEntity>
}
