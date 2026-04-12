package pl.zieleeksw.quizmi.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pl.zieleeksw.quizmi.IntegrationTest
import pl.zieleeksw.quizmi.user.domain.BootstrapAdminInitializer
import pl.zieleeksw.quizmi.user.domain.UserRepository
import pl.zieleeksw.quizmi.user.domain.UserRole
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(
    properties = [
        "app.bootstrap.admin.enabled=true",
        "app.bootstrap.admin.email=admin@quizmi.app",
        "app.bootstrap.admin.password=bootstrapAdmin123"
    ]
)
class BootstrapAdminIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var bootstrapAdminInitializer: BootstrapAdminInitializer

    @Test
    fun `should create bootstrap admin on startup and allow login`() {
        val savedUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow()

        assertEquals(UserRole.ADMIN, savedUser.role)
        assertTrue(passwordEncoder.matches(ADMIN_PASSWORD, savedUser.passwordHash))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(authenticationRequestJson())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL))
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andExpect(jsonPath("$.accessToken.value").isString)
            .andExpect(jsonPath("$.refreshToken.value").isString)
    }

    @Test
    fun `should not duplicate bootstrap admin when initializer runs again`() {
        val initialAdminId = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().id

        bootstrapAdminInitializer.run(DefaultApplicationArguments())

        val savedUsers = userRepository.findAll()

        assertEquals(1, savedUsers.size)
        assertEquals(initialAdminId, savedUsers.single().id)
        assertEquals(UserRole.ADMIN, savedUsers.single().role)
        assertTrue(passwordEncoder.matches(ADMIN_PASSWORD, savedUsers.single().passwordHash))
    }

    companion object {
        private const val ADMIN_EMAIL = "admin@quizmi.app"
        private const val ADMIN_PASSWORD = "bootstrapAdmin123"

        private fun authenticationRequestJson(): String {
            return """{"email":"$ADMIN_EMAIL","password":"$ADMIN_PASSWORD"}"""
        }
    }
}
