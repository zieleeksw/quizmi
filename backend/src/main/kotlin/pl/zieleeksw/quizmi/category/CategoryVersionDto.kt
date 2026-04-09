package pl.zieleeksw.quizmi.category

import java.time.Instant

data class CategoryVersionDto(
    val id: Long,
    val categoryId: Long,
    val versionNumber: Int,
    val name: String,
    val createdAt: Instant
)
