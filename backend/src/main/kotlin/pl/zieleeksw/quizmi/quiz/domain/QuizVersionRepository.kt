package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface QuizVersionRepository : JpaRepository<QuizVersionEntity, Long> {
    fun findByQuizIdAndVersionNumber(quizId: Long, versionNumber: Int): Optional<QuizVersionEntity>
    fun findAllByQuizIdOrderByVersionNumberDesc(quizId: Long): List<QuizVersionEntity>
}
