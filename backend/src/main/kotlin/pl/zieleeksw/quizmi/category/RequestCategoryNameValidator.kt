package pl.zieleeksw.quizmi.category

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.category.domain.CategoryNameValidator

@Component
class RequestCategoryNameValidator(
    private val categoryNameValidator: CategoryNameValidator
) : ConstraintValidator<ValidCategoryName, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            categoryNameValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(exception.message ?: "Category name is invalid.")
                .addConstraintViolation()

            false
        }
    }
}
