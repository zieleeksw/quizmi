package pl.zieleeksw.quizmi

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import pl.zieleeksw.quizmi.auth.domain.InvalidRefreshTokenException
import pl.zieleeksw.quizmi.category.domain.CategoryNotFoundException
import pl.zieleeksw.quizmi.course.domain.CourseNotFoundException
import pl.zieleeksw.quizmi.question.domain.QuestionNotFoundException
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

    @ExceptionHandler(CourseNotFoundException::class)
    fun handleCourseNotFoundException(
        exception: CourseNotFoundException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "CourseNotFoundException",
                message = exception.message ?: "Course was not found."
            )
        )
    }

    @ExceptionHandler(CategoryNotFoundException::class)
    fun handleCategoryNotFoundException(
        exception: CategoryNotFoundException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "CategoryNotFoundException",
                message = exception.message ?: "Category was not found."
            )
        )
    }

    @ExceptionHandler(QuestionNotFoundException::class)
    fun handleQuestionNotFoundException(
        exception: QuestionNotFoundException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "QuestionNotFoundException",
                message = exception.message ?: "Question was not found."
            )
        )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        exception: AuthenticationException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "AuthenticationException",
                message = "Invalid email or password."
            )
        )
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshTokenException(
        exception: InvalidRefreshTokenException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "InvalidRefreshTokenException",
                message = exception.message ?: "Refresh token is invalid or expired."
            )
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        exception: AccessDeniedException
    ): ResponseEntity<RuntimeExceptionDto> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            RuntimeExceptionDto(
                exception = exception::class.simpleName ?: "AccessDeniedException",
                message = "Access denied."
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
