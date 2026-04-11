package pl.zieleeksw.quizmi.quiz

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class QuizOrderMode(
    @get:JsonValue val value: String
) {
    FIXED("fixed"),
    RANDOM("random");

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromValue(value: String?): QuizOrderMode {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Quiz order mode is invalid.")
        }
    }
}
