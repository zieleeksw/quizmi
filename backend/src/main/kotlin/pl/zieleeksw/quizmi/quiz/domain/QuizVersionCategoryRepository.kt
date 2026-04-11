package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuizVersionCategoryRepository : JpaRepository<QuizVersionCategoryEntity, Long> {
    fun findAllByQuizVersionIdOrderByDisplayOrderAsc(quizVersionId: Long): List<QuizVersionCategoryEntity>
}
