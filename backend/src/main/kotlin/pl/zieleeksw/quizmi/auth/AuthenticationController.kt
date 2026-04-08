package pl.zieleeksw.quizmi.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@RestController
@RequestMapping("/auth")
class AuthenticationController(
    private val userFacade: UserFacade
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody @Valid request: RegisterUserRequest
    ): UserDto {
        val email = request.email ?: throw IllegalArgumentException("Email address cannot be empty.")
        val password = request.password ?: throw IllegalArgumentException("Password cannot be empty.")

        return userFacade.registerUser(
            email = email,
            password = password
        )
    }
}
