package pl.zieleeksw.quizmi.auth.domain

import org.springframework.stereotype.Service
import pl.zieleeksw.quizmi.auth.AuthenticationDto
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@Service
class AuthenticationFacade(
    private val userFacade: UserFacade,
    private val userAuthenticator: UserAuthenticator,
    private val tokenRefresher: TokenRefresher
) {

    fun register(
        email: String,
        password: String
    ): UserDto {
        return userFacade.registerUser(email, password)
    }

    fun login(
        email: String,
        password: String
    ): AuthenticationDto {
        return userAuthenticator.authenticate(email, password)
    }

    fun refreshToken(token: String): AuthenticationDto {
        return tokenRefresher.refresh(token)
    }

    fun currentUser(email: String): UserDto {
        return userFacade.findUserByEmailOrThrow(email)
    }
}
