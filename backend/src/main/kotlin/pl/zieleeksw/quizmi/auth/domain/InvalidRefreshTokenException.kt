package pl.zieleeksw.quizmi.auth.domain

class InvalidRefreshTokenException(
    override val message: String = "Refresh token is invalid or expired."
) : RuntimeException(message)
