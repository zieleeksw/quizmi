package pl.zieleeksw.quizmi.category

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

class CategoryIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should create category for course owner`() {
        val authentication = registerAndLogin(email = "category.owner@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)

        mockMvc.perform(
            post("/courses/{courseId}/categories", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"name":"HTTP Fundamentals"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.courseId").value(courseId))
            .andExpect(jsonPath("$.name").value("HTTP Fundamentals"))
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `should fetch course categories for any authenticated user`() {
        val owner = registerAndLogin(email = "category.list.owner@quizmi.app")
        val viewer = registerAndLogin(email = "category.list.viewer@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)

        (1..11).forEach { index ->
            createCategory(courseId, owner.accessToken, "Category $index")
        }

        mockMvc.perform(
            get("/courses/{courseId}/categories", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(9))
            .andExpect(jsonPath("$[0].courseId").value(courseId))
            .andExpect(jsonPath("$[8].courseId").value(courseId))
    }

    @Test
    fun `should allow moderator to update category`() {
        val owner = registerAndLogin(email = "category.moderator.owner@quizmi.app")
        val moderator = registerAndLogin(email = "category.moderator.member@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")

        requestAndApproveCourseJoin(courseId, moderator, owner)
        promoteToModerator(courseId, moderator, owner)

        mockMvc.perform(
            put("/courses/{courseId}/categories/{categoryId}", courseId, categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${moderator.accessToken}")
                .content("""{"name":"Authentication Flows"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(categoryId))
            .andExpect(jsonPath("$.name").value("Authentication Flows"))
    }

    @Test
    fun `should fetch category details for owner`() {
        val authentication = registerAndLogin(email = "category.details@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")

        mockMvc.perform(
            get("/courses/{courseId}/categories/{categoryId}", courseId, categoryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(categoryId))
            .andExpect(jsonPath("$.name").value("Authentication"))
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `should update category and append version history`() {
        val authentication = registerAndLogin(email = "category.update@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")

        mockMvc.perform(
            put("/courses/{courseId}/categories/{categoryId}", courseId, categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"name":"Authentication Flows"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(categoryId))
            .andExpect(jsonPath("$.name").value("Authentication Flows"))

        mockMvc.perform(
            get("/courses/{courseId}/categories/{categoryId}/versions", courseId, categoryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].versionNumber").value(2))
            .andExpect(jsonPath("$[0].name").value("Authentication Flows"))
            .andExpect(jsonPath("$[1].versionNumber").value(1))
            .andExpect(jsonPath("$[1].name").value("Authentication"))
    }

    @Test
    fun `should forbid category update for different owner`() {
        val owner = registerAndLogin(email = "category.private.owner@quizmi.app")
        val outsider = registerAndLogin(email = "category.outsider@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")

        mockMvc.perform(
            put("/courses/{courseId}/categories/{categoryId}", courseId, categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${outsider.accessToken}")
                .content("""{"name":"Authentication Flows"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should reject invalid category request`() {
        val authentication = registerAndLogin(email = "category.validation@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)

        mockMvc.perform(
            post("/courses/{courseId}/categories", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"name":"a"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.exception").value("MethodArgumentNotValidException"))
    }

    @Test
    fun `should reject duplicate category name in the same course`() {
        val authentication = registerAndLogin(email = "category.duplicate@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)

        createCategory(courseId, authentication.accessToken, "Spring Security")

        mockMvc.perform(
            post("/courses/{courseId}/categories", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"name":"spring security"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Category name already exists in this course."))
    }

    @Test
    fun `should reject category update without changing name`() {
        val authentication = registerAndLogin(email = "category.same-name@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")

        mockMvc.perform(
            put("/courses/{courseId}/categories/{categoryId}", courseId, categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"name":"authentication"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Category update must change the name."))
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

    private fun createCategory(
        courseId: Long,
        accessToken: String,
        name: String
    ) {
        mockMvc.perform(
            post("/courses/{courseId}/categories", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content("""{"name":"$name"}""")
        )
            .andExpect(status().isCreated)
    }

    private fun createCategoryAndReadId(
        courseId: Long,
        accessToken: String,
        name: String
    ): Long {
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

    private fun requestAndApproveCourseJoin(
        courseId: Long,
        requester: AuthIdentity,
        approver: AuthIdentity
    ) {
        mockMvc.perform(
            post("/courses/{courseId}/join-requests", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/courses/{courseId}/members/{memberUserId}/approve", courseId, requester.userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${approver.accessToken}")
        )
            .andExpect(status().isOk)
    }

    private fun promoteToModerator(
        courseId: Long,
        member: AuthIdentity,
        owner: AuthIdentity
    ) {
        mockMvc.perform(
            put("/courses/{courseId}/members/{memberUserId}/role", courseId, member.userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
                .content("""{"role":"MODERATOR"}""")
        )
            .andExpect(status().isOk)
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

    private fun String.readJsonNumber(
        objectName: String,
        fieldName: String
    ): Long {
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
