package pl.zieleeksw.quizmi.auth

data class RegisterUserRequest(
    @field:ValidEmail
    val email: String?,
    @field:ValidPassword
    val password: String?
)
