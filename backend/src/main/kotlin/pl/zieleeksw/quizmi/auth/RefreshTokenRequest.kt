package pl.zieleeksw.quizmi.auth

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token cannot be empty.")
    val token: String?
)
