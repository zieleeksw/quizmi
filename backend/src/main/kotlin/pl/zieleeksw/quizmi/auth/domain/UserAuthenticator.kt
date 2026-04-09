package pl.zieleeksw.quizmi.auth.domain

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import pl.zieleeksw.quizmi.auth.AuthenticationDto
import pl.zieleeksw.quizmi.auth.JwtDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@Service
class UserAuthenticator(
    private val authenticationManager: AuthenticationManager,
    private val jwtFacade: JwtFacade,
    private val userFacade: UserFacade
) {

    fun authenticate(
        email: String,
        password: String
    ): AuthenticationDto {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(email, password)
        )

        val user = userFacade.findUserByEmailOrThrow(email)

        return AuthenticationDto(
            user = user,
            accessToken = JwtDto(jwtFacade.generateAccessToken(email)),
            refreshToken = JwtDto(jwtFacade.generateRefreshToken(email))
        )
    }
}
