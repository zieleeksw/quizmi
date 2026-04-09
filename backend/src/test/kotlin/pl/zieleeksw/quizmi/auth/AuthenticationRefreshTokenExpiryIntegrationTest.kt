package pl.zieleeksw.quizmi.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pl.zieleeksw.quizmi.IntegrationTest

@TestPropertySource(
    properties = [
        "app.security.jwt.expiration.access-token-ms=5000",
        "app.security.jwt.expiration.refresh-token-ms=1200"
    ]
)
class AuthenticationRefreshTokenExpiryIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should reject expired refresh token`() {
        registerUser()
        val refreshToken = login().refreshToken

        Thread.sleep(1600)

        mockMvc.perform(
            post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$refreshToken"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired."))
    }

    private fun registerUser() {
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authenticationRequestJson())
        )
            .andExpect(status().isCreated)
    }

    private fun login(): AuthTokens {
        val response = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authenticationRequestJson())
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return AuthTokens(
            accessToken = response.readJsonValue("accessToken"),
            refreshToken = response.readJsonValue("refreshToken")
        )
    }

    private fun authenticationRequestJson(
        email: String = VALID_EMAIL,
        password: String = VALID_PASSWORD
    ): String {
        return """{"email":"$email","password":"$password"}"""
    }

    private fun String.readJsonValue(fieldName: String): String {
        val pattern = """"$fieldName"\s*:\s*\{\s*"value"\s*:\s*"([^"]+)"""".toRegex()
        val match = pattern.find(this)
            ?: throw IllegalStateException("Field $fieldName was not found in response: $this")

        return match.groupValues[1]
    }

    private data class AuthTokens(
        val accessToken: String,
        val refreshToken: String
    )

    companion object {
        private const val VALID_EMAIL = "refresh.expiry@quizmi.app"
        private const val VALID_PASSWORD = "password12345678"
    }
}
