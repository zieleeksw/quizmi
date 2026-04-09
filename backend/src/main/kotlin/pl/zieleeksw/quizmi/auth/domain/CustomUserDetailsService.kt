package pl.zieleeksw.quizmi.auth.domain

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import pl.zieleeksw.quizmi.user.domain.UserRepository

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("Invalid email or password.") }

        return SecurityUser(
            id = user.id!!,
            email = user.email,
            password = user.passwordHash,
            role = user.role
        )
    }
}
