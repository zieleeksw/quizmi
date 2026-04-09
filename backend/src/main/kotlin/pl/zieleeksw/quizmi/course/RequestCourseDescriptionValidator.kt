package pl.zieleeksw.quizmi.course

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.course.domain.CourseDescriptionValidator

@Component
class RequestCourseDescriptionValidator(
    private val courseDescriptionValidator: CourseDescriptionValidator
) : ConstraintValidator<ValidCourseDescription, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            courseDescriptionValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(exception.message ?: "Course description is invalid.")
                .addConstraintViolation()

            false
        }
    }
}
