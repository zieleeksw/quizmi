package pl.zieleeksw.quizmi.auth.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
class JwtProperties {
    var secretKey: String = ""
    var expiration: Expiration = Expiration()

    class Expiration {
        var accessTokenMs: Long = 900_000
        var refreshTokenMs: Long = 604_800_000
    }
}
