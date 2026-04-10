package pl.zieleeksw.quizmi.question

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.question.domain.QuestionCategoryIdsValidator

@Component
class RequestQuestionCategoryIdsValidator(
    private val questionCategoryIdsValidator: QuestionCategoryIdsValidator
) : ConstraintValidator<ValidQuestionCategoryIds, List<Long>?> {

    override fun isValid(
        value: List<Long>?,
        context: ConstraintValidatorContext
    ): Boolean {
        return try {
            questionCategoryIdsValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(exception.message ?: "Question category ids are invalid.")
                .addConstraintViolation()
            false
        }
    }
}
