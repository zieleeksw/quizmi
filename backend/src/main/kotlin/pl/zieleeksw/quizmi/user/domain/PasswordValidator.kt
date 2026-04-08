package pl.zieleeksw.quizmi.user.domain

import org.springframework.stereotype.Component

@Component
class PasswordValidator {

    companion object {
        private const val MIN_LENGTH = 12
        private const val MAX_LENGTH = 128
    }

    fun validate(password: String?) {
        val currentPassword = password ?: throw IllegalArgumentException("Password cannot be empty.")

        if (currentPassword.isEmpty()) {
            throw IllegalArgumentException("Password cannot be empty.")
        }

        if (currentPassword.length > MAX_LENGTH) {
            throw IllegalArgumentException("Password is too long. Max length is $MAX_LENGTH characters.")
        }

        if (currentPassword.length < MIN_LENGTH) {
            throw IllegalArgumentException("Password is too short. Min length is $MIN_LENGTH characters.")
        }
    }
}
