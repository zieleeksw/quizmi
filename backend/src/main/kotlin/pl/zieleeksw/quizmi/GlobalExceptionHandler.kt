package pl.zieleeksw.quizmi

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import pl.zieleeksw.quizmi.user.domain.EmailAlreadyExistsException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException
    ): ResponseEntity<FieldValidationErrorDto> {
        val fieldErrors = exception.bindingResult.fieldErrors.map { error ->
            FieldValidationErrorDto.FieldErrorDto(
                field = error.field,
                message = error.defaultMessage ?: "Validation error."
            )
        }

        return ResponseEntity.badRequest().body(
            FieldValidationErrorDto(
                exception = exception::class.simpleName ?: "MethodArgumentNotValidException",
                errors = fieldErrors
            )
        )
    }

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExistsException(
        exception: EmailAlreadyExistsException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "EmailAlreadyExistsException",
                message = exception.message ?: "Email already exists."
            )
        )
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        exception: RuntimeException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.badRequest().body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "RuntimeException",
                message = exception.message ?: "Unexpected runtime error."
            )
        )
    }
}
