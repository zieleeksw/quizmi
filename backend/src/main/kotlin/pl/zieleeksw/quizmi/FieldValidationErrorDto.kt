package pl.zieleeksw.quizmi

import java.time.Instant

data class FieldValidationErrorDto(
    val timestamp: Instant = Instant.now(),
    val exception: String,
    val errors: List<FieldErrorDto>
) {
    data class FieldErrorDto(
        val field: String,
        val message: String
    )
}
