package pl.zieleeksw.quizmi.category.domain

import org.springframework.data.jpa.repository.JpaRepository

interface CategoryVersionRepository : JpaRepository<CategoryVersionEntity, Long> {
    fun findAllByCategoryIdOrderByVersionNumberDesc(categoryId: Long): List<CategoryVersionEntity>
    fun findTopByCategoryIdOrderByVersionNumberDesc(categoryId: Long): CategoryVersionEntity?
}
