package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuizVersionQuestionRepository : JpaRepository<QuizVersionQuestionEntity, Long> {
    fun findAllByQuizVersionIdOrderByDisplayOrderAsc(quizVersionId: Long): List<QuizVersionQuestionEntity>
}
