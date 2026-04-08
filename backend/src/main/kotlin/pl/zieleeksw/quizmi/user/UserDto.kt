package pl.zieleeksw.quizmi.user

import pl.zieleeksw.quizmi.user.domain.UserRole

data class UserDto(
    val id: Long,
    val email: String,
    val role: UserRole
)
