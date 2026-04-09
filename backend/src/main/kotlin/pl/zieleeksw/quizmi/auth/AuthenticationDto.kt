package pl.zieleeksw.quizmi.auth

import pl.zieleeksw.quizmi.user.UserDto

data class AuthenticationDto(
    val user: UserDto,
    val accessToken: JwtDto,
    val refreshToken: JwtDto
)
