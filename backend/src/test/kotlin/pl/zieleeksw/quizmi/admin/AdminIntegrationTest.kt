package pl.zieleeksw.quizmi.admin

import org.hamcrest.Matchers.hasItem
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
    fun `should list quiz attempts across all courses and players for an admin, newest first`() {
        val owner = registerAndLogin(email = "attempts.owner@quizmi.app")
        val player = registerAndLogin(email = "attempts.player@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        saveAttempt(courseId, player.userId, quizId = 1L, quizTitle = "Filters and JWTs", finishedAt = "2026-09-01T10:00:00Z")

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].courseId").value(courseId))
            .andExpect(jsonPath("$.items[0].courseName").value("Spring Security Associate"))
            .andExpect(jsonPath("$.items[0].quizId").value(1))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Filters and JWTs"))
            .andExpect(jsonPath("$.items[0].userId").value(player.userId))
            .andExpect(jsonPath("$.items[0].playerEmail").value("attempts.player@quizmi.app"))
            .andExpect(jsonPath("$.items[0].correctAnswers").value(4))
            .andExpect(jsonPath("$.items[0].totalQuestions").value(5))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
    }

    @Test
    fun `should paginate quiz attempts using page and size query params`() {
        val owner = registerAndLogin(email = "paging.owner@quizmi.app")
        val player = registerAndLogin(email = "paging.player@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Docker Basics",
            description = "Learn local containers, images, and compose workflows."
        )

        saveAttempt(courseId, player.userId, quizId = 1L, quizTitle = "Oldest quiz", finishedAt = "2026-09-01T10:00:00Z")
        saveAttempt(courseId, player.userId, quizId = 2L, quizTitle = "Middle quiz", finishedAt = "2026-09-02T10:00:00Z")
        saveAttempt(courseId, player.userId, quizId = 3L, quizTitle = "Newest quiz", finishedAt = "2026-09-03T10:00:00Z")

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .param("page", "0")
                .param("size", "2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Newest quiz"))
            .andExpect(jsonPath("$.items[1].quizTitle").value("Middle quiz"))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(2))
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false))

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .param("page", "1")
                .param("size", "2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Oldest quiz"))
            .andExpect(jsonPath("$.pageNumber").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
    }

    @Test
    fun `should filter quiz attempts by course, quiz, and player, combinable with AND semantics`() {
        val ownerA = registerAndLogin(email = "filters.ownerA@quizmi.app")
        val ownerB = registerAndLogin(email = "filters.ownerB@quizmi.app")
        val playerX = registerAndLogin(email = "filters.playerx@quizmi.app")
        val playerY = registerAndLogin(email = "filters.playery@quizmi.app")

        val courseAId = createCourseAndReadId(ownerA.accessToken, "Course Alpha", "First course for filter tests.")
        val courseBId = createCourseAndReadId(ownerB.accessToken, "Course Beta", "Second course for filter tests.")

        saveAttempt(courseAId, playerX.userId, quizId = 10L, quizTitle = "Alpha Quiz One", finishedAt = "2026-09-01T10:00:00Z")
        saveAttempt(courseAId, playerY.userId, quizId = 11L, quizTitle = "Alpha Quiz Two", finishedAt = "2026-09-02T10:00:00Z")
        saveAttempt(courseBId, playerX.userId, quizId = 20L, quizTitle = "Beta Quiz One", finishedAt = "2026-09-03T10:00:00Z")

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .param("courseId", courseAId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Alpha Quiz Two"))
            .andExpect(jsonPath("$.items[1].quizTitle").value("Alpha Quiz One"))

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .param("quizId", "10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Alpha Quiz One"))

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .param("userId", playerX.userId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Beta Quiz One"))
            .andExpect(jsonPath("$.items[1].quizTitle").value("Alpha Quiz One"))

        mockMvc.perform(
            get("/admin/quiz-attempts")
                .param("courseId", courseAId.toString())
                .param("userId", playerX.userId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].quizTitle").value("Alpha Quiz One"))
    }

    @Test
    fun `should list distinct course and quiz options for filter dropdowns`() {
        val owner = registerAndLogin(email = "options.owner@quizmi.app")
        val player = registerAndLogin(email = "options.player@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken, "Options Course", "Course used for dropdown option tests.")

        saveAttempt(courseId, player.userId, quizId = 30L, quizTitle = "Options Quiz", finishedAt = "2026-09-05T10:00:00Z")

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/courses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].courseId").value(courseId))
            .andExpect(jsonPath("$[0].courseName").value("Options Course"))

        mockMvc.perform(
            get("/admin/quizzes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].quizId").value(30))
            .andExpect(jsonPath("$[0].quizTitle").value("Options Quiz"))
            .andExpect(jsonPath("$[0].courseId").value(courseId))
            .andExpect(jsonPath("$[0].courseName").value("Options Course"))
    }

    @Test
    fun `should scope course and quiz filter options to the selected player and course`() {
        val ownerA = registerAndLogin(email = "scoped.ownerA@quizmi.app")
        val ownerB = registerAndLogin(email = "scoped.ownerB@quizmi.app")
        val playerX = registerAndLogin(email = "scoped.playerx@quizmi.app")
        val playerY = registerAndLogin(email = "scoped.playery@quizmi.app")

        val courseAId = createCourseAndReadId(ownerA.accessToken, "Course Alpha", "First course for scoped option tests.")
        val courseBId = createCourseAndReadId(ownerB.accessToken, "Course Beta", "Second course for scoped option tests.")

        saveAttempt(courseAId, playerX.userId, quizId = 100L, quizTitle = "Alpha Quiz X", finishedAt = "2026-09-01T10:00:00Z")
        saveAttempt(courseAId, playerY.userId, quizId = 101L, quizTitle = "Alpha Quiz Y", finishedAt = "2026-09-02T10:00:00Z")
        saveAttempt(courseBId, playerY.userId, quizId = 200L, quizTitle = "Beta Quiz Y", finishedAt = "2026-09-03T10:00:00Z")

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/courses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        mockMvc.perform(
            get("/admin/courses")
                .param("userId", playerX.userId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].courseName").value("Course Alpha"))

        mockMvc.perform(
            get("/admin/courses")
                .param("userId", playerY.userId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        mockMvc.perform(
            get("/admin/quizzes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))

        mockMvc.perform(
            get("/admin/quizzes")
                .param("userId", playerX.userId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].quizTitle").value("Alpha Quiz X"))

        mockMvc.perform(
            get("/admin/quizzes")
                .param("courseId", courseAId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].quizTitle").value("Alpha Quiz X"))
            .andExpect(jsonPath("$[1].quizTitle").value("Alpha Quiz Y"))

        mockMvc.perform(
            get("/admin/quizzes")
                .param("userId", playerY.userId.toString())
                .param("courseId", courseAId.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].quizTitle").value("Alpha Quiz Y"))
    }

    @Test
    fun `should search users by email substring for the player filter`() {
        registerAndLogin(email = "search.testplayer@quizmi.app")
        registerAndLogin(email = "search.anotherplayer@quizmi.app")
        registerAndLogin(email = "search.unrelated@quizmi.app")

        val adminAccessToken = loginAsAdmin()

        mockMvc.perform(
            get("/admin/users")
                .param("search", "player")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].email", hasItem("search.testplayer@quizmi.app")))
            .andExpect(jsonPath("$[*].email", hasItem("search.anotherplayer@quizmi.app")))
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

    private fun saveAttempt(
        courseId: Long,
        userId: Long,
        quizId: Long,
        quizTitle: String,
        finishedAt: String,
        correctAnswers: Int = 4,
        totalQuestions: Int = 5
    ) {
        quizAttemptRepository.save(
            QuizAttemptEntity(
                courseId = courseId,
                quizId = quizId,
                userId = userId,
                quizTitle = quizTitle,
                correctAnswers = correctAnswers,
                totalQuestions = totalQuestions,
                reviewSnapshotJson = "[]",
                finishedAt = Instant.parse(finishedAt)
            )
        )
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
