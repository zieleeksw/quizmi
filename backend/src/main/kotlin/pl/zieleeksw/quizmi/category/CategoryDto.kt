package pl.zieleeksw.quizmi.category

import java.time.Instant

data class CategoryDto(
    val id: Long,
    val courseId: Long,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
