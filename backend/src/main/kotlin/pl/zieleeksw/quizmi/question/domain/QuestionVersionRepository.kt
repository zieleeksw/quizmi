package pl.zieleeksw.quizmi.question.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface QuestionVersionRepository : JpaRepository<QuestionVersionEntity, Long> {
    fun findAllByQuestionIdOrderByVersionNumberDesc(questionId: Long): List<QuestionVersionEntity>
    fun findByQuestionIdAndVersionNumber(questionId: Long, versionNumber: Int): Optional<QuestionVersionEntity>
}
