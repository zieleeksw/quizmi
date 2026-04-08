package pl.zieleeksw.quizmi.user.domain

class EmailAlreadyExistsException private constructor(
    message: String
) : RuntimeException(message) {

    companion object {
        fun forEmail(email: String): EmailAlreadyExistsException {
            return EmailAlreadyExistsException("User with email $email already exists.")
        }
    }
}
