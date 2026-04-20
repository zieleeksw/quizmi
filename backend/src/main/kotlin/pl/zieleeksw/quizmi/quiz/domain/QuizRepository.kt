package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuizRepository : JpaRepository<QuizEntity, Long> {
    fun findAllByCourseIdAndActiveTrueOrderByCreatedAtDesc(courseId: Long): List<QuizEntity>
    fun findTop3ByCourseIdAndActiveTrueOrderByCreatedAtDesc(courseId: Long): List<QuizEntity>
}
