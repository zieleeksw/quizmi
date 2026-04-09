package pl.zieleeksw.quizmi.auth

data class AuthenticationRequest(
    @field:ValidEmail
    val email: String?,
    @field:ValidPassword
    val password: String?
)
