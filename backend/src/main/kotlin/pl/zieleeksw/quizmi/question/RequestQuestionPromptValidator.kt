package pl.zieleeksw.quizmi.question

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.question.domain.QuestionPromptValidator

@Component
class RequestQuestionPromptValidator(
    private val questionPromptValidator: QuestionPromptValidator
) : ConstraintValidator<ValidQuestionPrompt, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            questionPromptValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(exception.message ?: "Question prompt is invalid.")
                .addConstraintViolation()
            false
        }
    }
}
