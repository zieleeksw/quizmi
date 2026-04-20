package pl.zieleeksw.quizmi.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.runtime.internal.AroundClosure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class ControllerExecutionTimeLoggingAspectTest {

    private val aspect = ControllerExecutionTimeLoggingAspect()
    private val logger = LoggerFactory.getLogger(ControllerExecutionTimeLoggingAspect::class.java) as Logger
    private val originalLevel = logger.level

    @AfterEach
    fun clearRequestContext() {
        logger.level = originalLevel
        logger.detachAndStopAllAppenders()
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `should log execution time for successful controller call`() {
        bindRequest("GET", "/api/health")
        val logAppender = attachAppender(Level.INFO)

        val result = aspect.logExecutionTime(
            TestProceedingJoinPoint(
                controllerClass = TestController::class.java,
                methodName = "health"
            ) { "UP" }
        )

        assertThat(result).isEqualTo("UP")
        assertThat(logAppender.formattedMessages())
            .anySatisfy { message ->
                assertThat(message).contains("Handled GET /api/health via TestController.health in")
            }
    }

    @Test
    fun `should log execution time for failed controller call`() {
        bindRequest("POST", "/courses")
        val logAppender = attachAppender(Level.WARN)

        try {
            aspect.logExecutionTime(
                TestProceedingJoinPoint(
                    controllerClass = TestController::class.java,
                    methodName = "create"
                ) { throw IllegalStateException("boom") }
            )
        } catch (_: IllegalStateException) {
            // Expected for this test case.
        }

        assertThat(logAppender.formattedMessages())
            .anySatisfy { message ->
                assertThat(message).contains("Failed POST /courses via TestController.create in")
                assertThat(message).contains("IllegalStateException")
            }
    }

    private fun bindRequest(
        method: String,
        uri: String
    ) {
        val request = MockHttpServletRequest(method, uri)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    private fun attachAppender(level: Level): ListAppender<ILoggingEvent> {
        logger.level = level
        return ListAppender<ILoggingEvent>().also { appender ->
            appender.start()
            logger.addAppender(appender)
        }
    }

    private fun ListAppender<ILoggingEvent>.formattedMessages(): List<String> {
        return list.map { it.formattedMessage }
    }

    private class TestController

    private class TestProceedingJoinPoint(
        private val controllerClass: Class<*>,
        private val methodName: String,
        private val invocation: () -> Any?
    ) : ProceedingJoinPoint {

        override fun proceed(): Any? = invocation()

        override fun proceed(args: Array<out Any?>?): Any? = invocation()

        override fun `set$AroundClosure`(arc: AroundClosure?) = Unit

        override fun getThis(): Any? = null

        override fun getTarget(): Any? = null

        override fun getArgs(): Array<Any?> = emptyArray()

        override fun getSignature(): Signature {
            return object : Signature {
                override fun toShortString(): String = "$methodName()"

                override fun toLongString(): String = "${controllerClass.name}.$methodName()"

                override fun getName(): String = methodName

                override fun getModifiers(): Int = 0

                override fun getDeclaringType(): Class<*> = controllerClass

                override fun getDeclaringTypeName(): String = controllerClass.name
            }
        }

        override fun toShortString(): String = "$methodName()"

        override fun toLongString(): String = "${controllerClass.name}.$methodName()"

        override fun getKind(): String = JoinPoint.METHOD_EXECUTION

        override fun getSourceLocation() = null

        override fun getStaticPart(): JoinPoint.StaticPart? = null
    }
}
