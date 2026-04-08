package pl.zieleeksw.quizmi.auth

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.user.domain.EmailValidator

@Component
class RequestEmailValidator(
    private val emailValidator: EmailValidator
) : ConstraintValidator<ValidEmail, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            emailValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(exception.message ?: "Email is not valid.")
                .addConstraintViolation()

            false
        }
    }
}
