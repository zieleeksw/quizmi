package pl.zieleeksw.quizmi.user.domain

import org.springframework.stereotype.Component

@Component
class EmailValidator {

    companion object {
        private const val EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$"
        private const val MAX_LENGTH = 255
        private const val MIN_LENGTH = 11
        private val PATTERN = Regex(EMAIL_REGEX)
    }

    fun validate(email: String?) {
        val currentEmail = email ?: throw IllegalArgumentException("Email address cannot be empty.")

        if (currentEmail.isEmpty()) {
            throw IllegalArgumentException("Email address cannot be empty.")
        }

        if (currentEmail.length > MAX_LENGTH) {
            throw IllegalArgumentException("Email is too long. Max length is $MAX_LENGTH characters.")
        }

        if (currentEmail.length < MIN_LENGTH) {
            throw IllegalArgumentException("Email is too short. Min length is $MIN_LENGTH characters.")
        }

        if (!PATTERN.matches(currentEmail)) {
            throw IllegalArgumentException("Email is invalid: $currentEmail")
        }
    }
}
