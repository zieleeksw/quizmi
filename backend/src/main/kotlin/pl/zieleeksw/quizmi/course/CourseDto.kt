package pl.zieleeksw.quizmi.course

import java.time.Instant

data class CourseDto(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Instant,
    val ownerUserId: Long
)
