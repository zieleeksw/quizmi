package pl.zieleeksw.quizmi.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.auth.domain.AuthenticationFacade
import pl.zieleeksw.quizmi.user.UserDto

@RestController
@RequestMapping("/auth")
class AuthenticationController(
    private val authenticationFacade: AuthenticationFacade
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody @Valid request: RegisterUserRequest
    ): UserDto {
        val email = request.email ?: throw IllegalArgumentException("Email address cannot be empty.")
        val password = request.password ?: throw IllegalArgumentException("Password cannot be empty.")

        return authenticationFacade.register(
            email = email,
            password = password
        )
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: AuthenticationRequest
    ): AuthenticationDto {
        val email = request.email ?: throw IllegalArgumentException("Email address cannot be empty.")
        val password = request.password ?: throw IllegalArgumentException("Password cannot be empty.")

        return authenticationFacade.login(
            email = email,
            password = password
        )
    }

    @PostMapping("/refresh-token")
    fun refreshToken(
        @RequestBody @Valid request: RefreshTokenRequest
    ): AuthenticationDto {
        val token = request.token ?: throw IllegalArgumentException("Refresh token cannot be empty.")

        return authenticationFacade.refreshToken(token)
    }

    @GetMapping("/me")
    fun me(authentication: Authentication): UserDto {
        return authenticationFacade.currentUser(authentication.name)
    }
}
