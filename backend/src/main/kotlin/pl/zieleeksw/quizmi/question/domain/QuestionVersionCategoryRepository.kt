package pl.zieleeksw.quizmi.question.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuestionVersionCategoryRepository : JpaRepository<QuestionVersionCategoryEntity, Long> {
    fun findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId: Long): List<QuestionVersionCategoryEntity>
    fun findAllByQuestionVersionIdInOrderByQuestionVersionIdAscDisplayOrderAsc(questionVersionIds: Collection<Long>): List<QuestionVersionCategoryEntity>
}
