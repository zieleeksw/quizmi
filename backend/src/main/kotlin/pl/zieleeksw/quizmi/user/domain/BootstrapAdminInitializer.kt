package pl.zieleeksw.quizmi.user.domain

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class BootstrapAdminInitializer(
    private val bootstrapAdminProperties: BootstrapAdminProperties,
    private val userFacade: UserFacade
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (!bootstrapAdminProperties.enabled) {
            return
        }

        val email = bootstrapAdminProperties.email.trim()
        val password = bootstrapAdminProperties.password

        if (email.isBlank()) {
            throw IllegalStateException("Bootstrap admin email must be configured when bootstrap admin is enabled.")
        }

        if (password.isBlank()) {
            throw IllegalStateException("Bootstrap admin password must be configured when bootstrap admin is enabled.")
        }

        val admin = userFacade.ensureBootstrapAdmin(
            email = email,
            password = password
        )

        logger.info("Bootstrap admin is ready for {}", admin.email)
    }
}
