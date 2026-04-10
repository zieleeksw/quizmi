package pl.zieleeksw.quizmi.question.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuestionRepository : JpaRepository<QuestionEntity, Long> {
    fun findAllByCourseIdOrderByCreatedAtDesc(courseId: Long): List<QuestionEntity>
}
