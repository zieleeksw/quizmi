package pl.zieleeksw.quizmi.category.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun findAllByCourseIdOrderByNameAsc(courseId: Long): List<CategoryEntity>
    fun existsByCourseIdAndNameIgnoreCase(courseId: Long, name: String): Boolean
}
