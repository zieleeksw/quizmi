package pl.zieleeksw.quizmi.question

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.question.domain.QuestionAnswersValidator

@Component
class RequestQuestionAnswersValidator(
    private val questionAnswersValidator: QuestionAnswersValidator
) : ConstraintValidator<ValidQuestionAnswers, List<QuestionAnswerRequest>?> {

    override fun isValid(
        value: List<QuestionAnswerRequest>?,
        context: ConstraintValidatorContext
    ): Boolean {
        return try {
            questionAnswersValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(exception.message ?: "Question answers are invalid.")
                .addConstraintViolation()
            false
        }
    }
}
