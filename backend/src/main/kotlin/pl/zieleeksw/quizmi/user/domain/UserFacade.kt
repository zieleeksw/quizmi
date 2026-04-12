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

        return savedUser.toDto()
    }

    @Transactional
    fun ensureBootstrapAdmin(
        email: String,
        password: String
    ): UserDto {
        emailValidator.validate(email)
        passwordValidator.validate(password)

        val existingUser = userRepository.findByEmail(email).orElse(null)

        if (existingUser != null) {
            val passwordMatches = passwordEncoder.matches(password, existingUser.passwordHash)

            if (existingUser.role != UserRole.ADMIN || !passwordMatches) {
                existingUser.applyBootstrapAdmin(passwordEncoder.encode(password)!!)
                return userRepository.save(existingUser).toDto()
            }

            return existingUser.toDto()
        }

        val savedUser = userRepository.save(
            UserEntity(
                email = email,
                passwordHash = passwordEncoder.encode(password)!!,
                role = UserRole.ADMIN
            )
        )

        return savedUser.toDto()
    }

    @Transactional(readOnly = true)
    fun findUserByEmailOrThrow(email: String): UserDto {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("User with email $email was not found.") }

        return user.toDto()
    }

    private fun UserEntity.toDto(): UserDto {
        return UserDto(
            id = id!!,
            email = email,
            role = role
        )
    }
}
