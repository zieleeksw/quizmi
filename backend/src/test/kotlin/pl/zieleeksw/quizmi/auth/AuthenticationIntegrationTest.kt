package pl.zieleeksw.quizmi.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pl.zieleeksw.quizmi.IntegrationTest

class AuthenticationIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should login on valid credentials and return tokens`() {
        registerUser()

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authenticationRequestJson())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.email").value(VALID_EMAIL))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andExpect(jsonPath("$.accessToken.value").isString)
            .andExpect(jsonPath("$.refreshToken.value").isString)
    }

    @Test
    fun `should reject login on invalid credentials`() {
        registerUser()

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authenticationRequestJson(password = "wrongPassword123"))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid email or password."))
    }

    @Test
    fun `should return current user for valid access token`() {
        registerUser()
        val authentication = login()

        mockMvc.perform(
            get("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(VALID_EMAIL))
            .andExpect(jsonPath("$.role").value("USER"))
    }

    @Test
    fun `should reject current user when access token is missing`() {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should refresh access token with valid refresh token`() {
        registerUser()
        val authentication = login()

        mockMvc.perform(
            post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"${authentication.refreshToken}"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.email").value(VALID_EMAIL))
            .andExpect(jsonPath("$.accessToken.value").isString)
            .andExpect(jsonPath("$.refreshToken.value").value(authentication.refreshToken))
    }

    @Test
    fun `should reject refresh token when invalid token is provided`() {
        mockMvc.perform(
            post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"invalid-refresh-token"}""")
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

        val accessToken = response.readJsonValue("accessToken")
        val refreshToken = response.readJsonValue("refreshToken")

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun String.readJsonValue(fieldName: String): String {
        val pattern = """"$fieldName"\s*:\s*\{\s*"value"\s*:\s*"([^"]+)"""".toRegex()
        val match = pattern.find(this)
            ?: throw IllegalStateException("Field $fieldName was not found in response: $this")

        return match.groupValues[1]
    }

    private fun authenticationRequestJson(
        email: String = VALID_EMAIL,
        password: String = VALID_PASSWORD
    ): String {
        return """{"email":"$email","password":"$password"}"""
    }

    private data class AuthTokens(
        val accessToken: String,
        val refreshToken: String
    )

    companion object {
        private const val VALID_EMAIL = "token.master@quizmi.app"
        private const val VALID_PASSWORD = "password12345678"
    }
}
