package pl.zieleeksw.quizmi.question

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pl.zieleeksw.quizmi.IntegrationTest

class QuestionIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should create question for course owner`() {
        val authentication = registerAndLogin("question.owner@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")

        mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"prompt":"Which flow returns a fresh access token to the browser?","answers":[{"content":"Refresh token flow","correct":true},{"content":"CORS preflight","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.courseId").value(courseId))
            .andExpect(jsonPath("$.currentVersionNumber").value(1))
            .andExpect(jsonPath("$.categories[0].id").value(categoryId))
            .andExpect(jsonPath("$.answers.length()").value(2))
    }

    @Test
    fun `should fetch questions and preview for any authenticated user`() {
        val owner = registerAndLogin("question.list@quizmi.app")
        val viewer = registerAndLogin("question.viewer@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val authCategoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")
        val jwtCategoryId = createCategoryAndReadId(courseId, owner.accessToken, "JWT")

        createQuestion(courseId, owner.accessToken, authCategoryId, "Which token should stay on the client for refresh requests?")
        createQuestion(courseId, owner.accessToken, jwtCategoryId, "Which claim usually carries the subject identifier?")

        mockMvc.perform(
            get("/courses/{courseId}/questions", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        mockMvc.perform(
            get("/courses/{courseId}/questions/preview", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
                .param("search", "subject")
                .param("categoryId", jwtCategoryId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].categories[0].id").value(jwtCategoryId))
    }

    @Test
    fun `should fetch question details and versions after update`() {
        val authentication = registerAndLogin("question.history@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionId = createQuestionAndReadId(
            courseId,
            authentication.accessToken,
            categoryId,
            "Which flow returns a fresh access token to the browser?"
        )

        mockMvc.perform(
            put("/courses/{courseId}/questions/{questionId}", courseId, questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"prompt":"Which backend flow returns a fresh access token to the browser?","answers":[{"content":"Refresh token flow","correct":true},{"content":"CORS preflight","correct":false},{"content":"Static file serving","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentVersionNumber").value(2))

        mockMvc.perform(
            get("/courses/{courseId}/questions/{questionId}", courseId, questionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(questionId))
            .andExpect(jsonPath("$.answers.length()").value(3))

        mockMvc.perform(
            get("/courses/{courseId}/questions/{questionId}/versions", courseId, questionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].versionNumber").value(2))
            .andExpect(jsonPath("$[1].versionNumber").value(1))
    }

    @Test
    fun `should forbid question update for different owner`() {
        val owner = registerAndLogin("question.private.owner@quizmi.app")
        val outsider = registerAndLogin("question.outsider@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")
        val questionId = createQuestionAndReadId(
            courseId,
            owner.accessToken,
            categoryId,
            "Which flow returns a fresh access token to the browser?"
        )

        mockMvc.perform(
            put("/courses/{courseId}/questions/{questionId}", courseId, questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${outsider.accessToken}")
                .content(
                    """
                    {"prompt":"Which backend flow returns a fresh access token to the browser?","answers":[{"content":"Refresh token flow","correct":true},{"content":"CORS preflight","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should reject invalid question request`() {
        val authentication = registerAndLogin("question.invalid@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")

        mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"prompt":"short","answers":[{"content":"Refresh token flow","correct":false},{"content":"CORS preflight","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.exception").value("MethodArgumentNotValidException"))
    }

    @Test
    fun `should reject duplicate answers in one question`() {
        val authentication = registerAndLogin("question.duplicate.answers@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")

        mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"prompt":"Which option should remain unique in a question builder?","answers":[{"content":"Refresh token flow","correct":true},{"content":" refresh token flow ","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.exception").value("MethodArgumentNotValidException"))
            .andExpect(jsonPath("$.errors[0].field").value("answers"))
            .andExpect(jsonPath("$.errors[0].message").value("Question answers must be unique."))
    }

    @Test
    fun `should reject question update without changes`() {
        val authentication = registerAndLogin("question.same@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionId = createQuestionAndReadId(
            courseId,
            authentication.accessToken,
            categoryId,
            "Which flow returns a fresh access token to the browser?"
        )

        mockMvc.perform(
            put("/courses/{courseId}/questions/{questionId}", courseId, questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"prompt":"Which flow returns a fresh access token to the browser?","answers":[{"content":"Refresh token flow","correct":true},{"content":"CORS preflight","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Question update must change the prompt, answers, or categories."))
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
            accessToken = loginResponse.readJsonValue("accessToken")
        )
    }

    private fun createCourseAndReadId(accessToken: String): Long {
        val response = mockMvc.perform(
            post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content("""{"name":"Spring Security Associate","description":"A focused course for filters, JWTs, and authorization workflows."}""")
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return response.readJsonNumber("id")
    }

    private fun createCategoryAndReadId(courseId: Long, accessToken: String, name: String): Long {
        val response = mockMvc.perform(
            post("/courses/{courseId}/categories", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content("""{"name":"$name"}""")
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return response.readJsonNumber("id")
    }

    private fun createQuestion(
        courseId: Long,
        accessToken: String,
        categoryId: Long,
        prompt: String
    ) {
        mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content(
                    """
                    {"prompt":"$prompt","answers":[{"content":"Refresh token flow","correct":true},{"content":"CORS preflight","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
    }

    private fun createQuestionAndReadId(
        courseId: Long,
        accessToken: String,
        categoryId: Long,
        prompt: String
    ): Long {
        val response = mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content(
                    """
                    {"prompt":"$prompt","answers":[{"content":"Refresh token flow","correct":true},{"content":"CORS preflight","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
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

    private data class AuthIdentity(
        val accessToken: String
    )
}
