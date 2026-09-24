package pl.zieleeksw.quizmi.admin

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pl.zieleeksw.quizmi.IntegrationTest
import pl.zieleeksw.quizmi.attempt.domain.QuizAttemptEntity
import pl.zieleeksw.quizmi.attempt.domain.QuizAttemptRepository
import java.time.Instant

@TestPropertySource(
    properties = [
        "app.bootstrap.admin.enabled=true",
        "app.bootstrap.admin.email=admin@quizmi.app",
        "app.bootstrap.admin.password=bootstrapAdmin123"
    ]
)
class AdminIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var quizAttemptRepository: QuizAttemptRepository

    @Test
    fun `should list quiz attempts across all courses and players for an admin`() {
        val owner = registerAndLogin(email = "attempts.owner@quizmi.app")
        val player = registerAndLogin(email = "attempts.player@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        quizAttemptRepository.save(
            QuizAttemptEntity(
                courseId = courseId,
                quizId = 1L,
                userId = player.userId,
                quizTitle = "Filters and JWTs",
                correctAnswers = 4,
                totalQuestions = 5,
                reviewSnapshotJson = "[]",
                finishedAt = Instant.parse("2026-09-01T10:00:00Z")
            )
        )

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].courseId").value(courseId))
            .andExpect(jsonPath("$[0].courseName").value("Spring Security Associate"))
            .andExpect(jsonPath("$[0].quizId").value(1))
            .andExpect(jsonPath("$[0].quizTitle").value("Filters and JWTs"))
            .andExpect(jsonPath("$[0].userId").value(player.userId))
            .andExpect(jsonPath("$[0].playerEmail").value("attempts.player@quizmi.app"))
            .andExpect(jsonPath("$[0].correctAnswers").value(4))
            .andExpect(jsonPath("$[0].totalQuestions").value(5))
    }

    @Test
    fun `should reject non-admin users with 403`() {
        val user = registerAndLogin(email = "attempts.non-admin@quizmi.app")

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should reject unauthenticated requests`() {
        mockMvc.perform(get("/admin/quiz-attempts"))
            .andExpect(status().isUnauthorized)
    }

    private fun loginAsAdmin(): String {
        val response = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@quizmi.app","password":"bootstrapAdmin123"}""")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return response.readJsonValue("accessToken")
    }

    private fun registerAndLogin(email: String): AuthIdentity {
        val password = "password12345678"

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
        )
            .andExpect(status().isCreated)

        val loginResponse = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return AuthIdentity(
            userId = loginResponse.readJsonNumber("user", "id"),
            accessToken = loginResponse.readJsonValue("accessToken")
        )
    }

    private fun createCourseAndReadId(
        accessToken: String,
        name: String,
        description: String
    ): Long {
        val response = mockMvc.perform(
            post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content("""{"name":"$name","description":"$description"}""")
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return response.readJsonNumber("id")
    }

    private fun String.readJsonValue(fieldName: String): String {
        val pattern = """"$fieldName"\s*:\s*\{\s*"value"\s*:\s*"([^"]+)"""".toRegex()
        val match = pattern.find(this)
            ?: throw IllegalStateException("Field $fieldName was not found in response: $this")

        return match.groupValues[1]
    }

    private fun String.readJsonNumber(fieldName: String): Long {
        val pattern = """"$fieldName"\s*:\s*(\d+)""".toRegex()
        val match = pattern.find(this)
            ?: throw IllegalStateException("Field $fieldName was not found in response: $this")

        return match.groupValues[1].toLong()
    }

    private fun String.readJsonNumber(objectName: String, fieldName: String): Long {
        val pattern = """"$objectName"\s*:\s*\{[^}]*"$fieldName"\s*:\s*(\d+)""".toRegex()
        val match = pattern.find(this)
            ?: throw IllegalStateException("Field $objectName.$fieldName was not found in response: $this")

        return match.groupValues[1].toLong()
    }

    private data class AuthIdentity(
        val userId: Long,
        val accessToken: String
    )
}
