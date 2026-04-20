package pl.zieleeksw.quizmi.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
@ConditionalOnProperty(
    prefix = "app.logging.controller-execution-time",
    name = ["enabled"],
    havingValue = "true"
)
class ControllerExecutionTimeLoggingAspect {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.nanoTime()
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val httpMethod = request?.method ?: "N/A"
        val requestUri = request?.requestURI ?: "N/A"
        val controllerName = joinPoint.signature.declaringType.simpleName
        val methodName = joinPoint.signature.name

        try {
            val result = joinPoint.proceed()
            logSuccess(httpMethod, requestUri, controllerName, methodName, start)
            return result
        } catch (exception: Throwable) {
            logFailure(httpMethod, requestUri, controllerName, methodName, start, exception)
            throw exception
        }
    }

    private fun logSuccess(
        httpMethod: String,
        requestUri: String,
        controllerName: String,
        methodName: String,
        start: Long
    ) {
        logger.info(
            "Handled {} {} via {}.{} in {} ms",
            httpMethod,
            requestUri,
            controllerName,
            methodName,
            elapsedMillis(start)
        )
    }

    private fun logFailure(
        httpMethod: String,
        requestUri: String,
        controllerName: String,
        methodName: String,
        start: Long,
        exception: Throwable
    ) {
        logger.warn(
            "Failed {} {} via {}.{} in {} ms with {}",
            httpMethod,
            requestUri,
            controllerName,
            methodName,
            elapsedMillis(start),
            exception.javaClass.simpleName
        )
    }

    private fun elapsedMillis(start: Long): Long {
        return (System.nanoTime() - start) / 1_000_000
    }
}
