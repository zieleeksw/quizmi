package pl.zieleeksw.quizmi.quiz

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class QuizMode(
    @get:JsonValue val value: String
) {
    MANUAL("manual"),
    RANDOM("random"),
    CATEGORY("category");

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromValue(value: String?): QuizMode {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Quiz mode is invalid.")
        }
    }
}
