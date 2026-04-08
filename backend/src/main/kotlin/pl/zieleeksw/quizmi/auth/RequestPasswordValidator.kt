package pl.zieleeksw.quizmi.auth

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import pl.zieleeksw.quizmi.user.domain.PasswordValidator

@Component
class RequestPasswordValidator(
    private val passwordValidator: PasswordValidator
) : ConstraintValidator<ValidPassword, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return try {
            passwordValidator.validate(value)
            true
        } catch (exception: IllegalArgumentException) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(exception.message ?: "Password is not valid.")
                .addConstraintViolation()

            false
        }
    }
}
