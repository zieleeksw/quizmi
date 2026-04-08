package pl.zieleeksw.quizmi.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pl.zieleeksw.quizmi.IntegrationTest
import pl.zieleeksw.quizmi.user.domain.UserRepository
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UserRegistrationIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `should register new user and hash password`() {
        val email = "abstract.simon.code@gmail.com"
        val request = RegisterUserRequest(
            email = email,
            password = VALID_PASSWORD
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestJson(request.email, request.password))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("USER"))

        val savedUser = userRepository.findByEmail(email).orElseThrow()

        assertNotEquals(VALID_PASSWORD, savedUser.passwordHash)
        assertTrue(passwordEncoder.matches(VALID_PASSWORD, savedUser.passwordHash))
    }

    @Test
    fun `should return conflict when email already exists`() {
        val email = "abstract.simon.code.valid@gmail.com"
        val request = RegisterUserRequest(
            email = email,
            password = VALID_PASSWORD
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestJson(request.email, request.password))
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestJson(request.email, request.password))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.exception").value("EmailAlreadyExistsException"))
            .andExpect(jsonPath("$.message").value("User with email $email already exists."))

        assertEquals(1, userRepository.count())
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationRequests")
    fun `should reject invalid registration request`(
        email: String?,
        password: String?,
        expectedField: String,
        expectedMessage: String
    ) {
        val request = RegisterUserRequest(email = email, password = password)

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestJson(request.email, request.password))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.exception").value("MethodArgumentNotValidException"))
            .andExpect(jsonPath("$.errors[0].field").value(expectedField))
            .andExpect(jsonPath("$.errors[0].message").value(expectedMessage))

        assertEquals(0, userRepository.count())
    }

    companion object {
        private const val VALID_PASSWORD = "password12345678"

        @JvmStatic
        fun invalidRegistrationRequests(): Stream<Arguments> {
            val validEmail = "java.is.love@cloud.it"

            return Stream.of(
                Arguments.of("a".repeat(256), VALID_PASSWORD, "email", "Email is too long. Max length is 255 characters."),
                Arguments.of("aaaaa", VALID_PASSWORD, "email", "Email is too short. Min length is 11 characters."),
                Arguments.of("", VALID_PASSWORD, "email", "Email address cannot be empty."),
                Arguments.of(null, VALID_PASSWORD, "email", "Email address cannot be empty."),
                Arguments.of("userdomain.com", VALID_PASSWORD, "email", "Email is invalid: userdomain.com"),
                Arguments.of("user@domain..com", VALID_PASSWORD, "email", "Email is invalid: user@domain..com"),
                Arguments.of("user@-domain.com", VALID_PASSWORD, "email", "Email is invalid: user@-domain.com"),
                Arguments.of(validEmail, "", "password", "Password cannot be empty."),
                Arguments.of(validEmail, null, "password", "Password cannot be empty."),
                Arguments.of(validEmail, "a".repeat(11), "password", "Password is too short. Min length is 12 characters."),
                Arguments.of(validEmail, "a".repeat(129), "password", "Password is too long. Max length is 128 characters.")
            )
        }

        private fun registerRequestJson(email: String?, password: String?): String {
            val emailJson = email?.let { "\"${escapeJson(it)}\"" } ?: "null"
            val passwordJson = password?.let { "\"${escapeJson(it)}\"" } ?: "null"

            return """{"email":$emailJson,"password":$passwordJson}"""
        }

        private fun escapeJson(value: String): String {
            return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        }
    }
}
