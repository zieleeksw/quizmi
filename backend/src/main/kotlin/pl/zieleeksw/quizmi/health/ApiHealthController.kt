package pl.zieleeksw.quizmi.health

import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class ApiHealthController(
    private val environment: Environment
) {

    @GetMapping
    fun health(): HealthResponse {
        val activeProfile = environment.activeProfiles.firstOrNull() ?: "default"

        return HealthResponse(
            status = "UP",
            application = "quizmi",
            profile = activeProfile
        )
    }
}

data class HealthResponse(
    val status: String,
    val application: String,
    val profile: String
)
