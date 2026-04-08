package pl.zieleeksw.quizmi.user.domain

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.user.UserDto

@Service
class UserFacade(
    private val userRepository: UserRepository,
    private val emailValidator: EmailValidator,
    private val passwordValidator: PasswordValidator,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun registerUser(
        email: String,
        password: String
    ): UserDto {
        emailValidator.validate(email)
        passwordValidator.validate(password)

        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyExistsException.forEmail(email)
        }

        val savedUser = userRepository.save(
            UserEntity(
                email = email,
                passwordHash = passwordEncoder.encode(password)!!,
                role = UserRole.USER
            )
        )

        return UserDto(
            id = savedUser.id!!,
            email = savedUser.email,
            role = savedUser.role
        )
    }
}
