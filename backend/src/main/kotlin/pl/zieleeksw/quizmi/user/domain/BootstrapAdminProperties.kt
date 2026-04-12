package pl.zieleeksw.quizmi.user.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.bootstrap.admin")
class BootstrapAdminProperties {
    var enabled: Boolean = false
    var email: String = ""
    var password: String = ""
}
