package pl.zieleeksw.quizmi.course

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.course.domain.CourseNameValidator

@Component
class RequestCourseNameValidator(
    private val courseNameValidator: CourseNameValidator
) : ConstraintValidator<ValidCourseName, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            courseNameValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(exception.message ?: "Course name is invalid.")
                .addConstraintViolation()

            false
        }
    }
}
