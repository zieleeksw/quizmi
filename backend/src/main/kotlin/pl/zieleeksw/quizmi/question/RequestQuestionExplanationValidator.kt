package pl.zieleeksw.quizmi.question

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.question.domain.QuestionExplanationValidator

@Component
class RequestQuestionExplanationValidator(
    private val questionExplanationValidator: QuestionExplanationValidator
) : ConstraintValidator<ValidQuestionExplanation, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            questionExplanationValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(exception.message ?: "Question explanation is invalid.")
                .addConstraintViolation()
            false
        }
    }
}
