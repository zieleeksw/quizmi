package pl.zieleeksw.quizmi.auth.domain

import org.springframework.stereotype.Service
import pl.zieleeksw.quizmi.auth.AuthenticationDto
import pl.zieleeksw.quizmi.auth.JwtDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@Service
class TokenRefresher(
    private val jwtFacade: JwtFacade,
    private val userFacade: UserFacade
) {

    fun refresh(refreshToken: String): AuthenticationDto {
        return try {
            performRefresh(refreshToken)
        } catch (_: Exception) {
            throw InvalidRefreshTokenException()
        }
    }

    private fun performRefresh(refreshToken: String): AuthenticationDto {
        val email = jwtFacade.extractEmail(refreshToken)
        val user = userFacade.findUserByEmailOrThrow(email)

        if (!jwtFacade.isRefreshTokenValid(refreshToken, user.email)) {
            throw InvalidRefreshTokenException()
        }

        return AuthenticationDto(
            user = user,
            accessToken = JwtDto(jwtFacade.generateAccessToken(user.email)),
            refreshToken = JwtDto(refreshToken)
        )
    }
}
