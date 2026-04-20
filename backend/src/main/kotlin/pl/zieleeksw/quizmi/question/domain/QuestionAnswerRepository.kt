package pl.zieleeksw.quizmi.question.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuestionAnswerRepository : JpaRepository<QuestionAnswerEntity, Long> {
    fun findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId: Long): List<QuestionAnswerEntity>
    fun findAllByQuestionVersionIdInOrderByQuestionVersionIdAscDisplayOrderAsc(questionVersionIds: Collection<Long>): List<QuestionAnswerEntity>
}
