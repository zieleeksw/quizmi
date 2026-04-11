package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.data.jpa.repository.JpaRepository

interface QuizRepository : JpaRepository<QuizEntity, Long> {
    fun findAllByCourseIdAndActiveTrueOrderByCreatedAtDesc(courseId: Long): List<QuizEntity>
}
