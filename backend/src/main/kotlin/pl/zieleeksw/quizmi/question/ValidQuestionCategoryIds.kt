package pl.zieleeksw.quizmi.question

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [RequestQuestionCategoryIdsValidator::class])
annotation class ValidQuestionCategoryIds(
    val message: String = "Question category ids are invalid.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
