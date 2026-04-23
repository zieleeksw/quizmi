package pl.zieleeksw.quizmi.feedback

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.ai.feedback")
class AiFeedbackProperties {
    var enabled: Boolean = false
    var baseUrl: String = "https://openrouter.ai/api/v1"
    var apiKey: String = ""
    var model: String = "google/gemma-3-4b-it:free"
    var maxTokens: Int = 220
    var appName: String = "Quizmi"
    var siteUrl: String = ""
}
