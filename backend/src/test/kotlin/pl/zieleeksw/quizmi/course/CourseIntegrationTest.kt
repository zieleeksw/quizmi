package pl.zieleeksw.quizmi.course

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

class CourseIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should create course for authenticated user`() {
        val authentication = registerAndLogin(
            email = "course.owner@quizmi.app"
        )

        mockMvc.perform(
            post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(createCourseRequestJson())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Spring Security Associate"))
            .andExpect(jsonPath("$.description").value("A focused course for filters, JWTs, and authorization workflows."))
            .andExpect(jsonPath("$.ownerUserId").value(authentication.userId))
    }

    @Test
    fun `should fetch only courses owned by authenticated user`() {
        val firstUser = registerAndLogin(email = "first.owner@quizmi.app")
        val secondUser = registerAndLogin(email = "second.owner@quizmi.app")

        createCourse(firstUser.accessToken, "Spring Boot Associate", "Build REST APIs and persistence with confidence.")
        createCourse(secondUser.accessToken, "Docker Basics", "Learn local containers, images, and compose workflows.")
        createCourse(firstUser.accessToken, "Spring Security Associate", "A focused course for filters, JWTs, and authorization workflows.")

        mockMvc.perform(
            get("/courses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${firstUser.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].ownerUserId").value(firstUser.userId))
            .andExpect(jsonPath("$[1].ownerUserId").value(firstUser.userId))
    }

    @Test
    fun `should fetch course details for owner`() {
        val authentication = registerAndLogin(email = "details.owner@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = authentication.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(courseId))
            .andExpect(jsonPath("$.name").value("Spring Security Associate"))
            .andExpect(jsonPath("$.description").value("A focused course for filters, JWTs, and authorization workflows."))
            .andExpect(jsonPath("$.ownerUserId").value(authentication.userId))
    }

    @Test
    fun `should update course details for owner`() {
        val authentication = registerAndLogin(email = "update.owner@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = authentication.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            put("/courses/{courseId}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """{"name":"Spring Security Mastery","description":"An updated course for advanced filters, JWTs, and authorization design."}"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(courseId))
            .andExpect(jsonPath("$.name").value("Spring Security Mastery"))
            .andExpect(jsonPath("$.description").value("An updated course for advanced filters, JWTs, and authorization design."))
    }

    @Test
    fun `should forbid fetching course details for different owner`() {
        val owner = registerAndLogin(email = "private.owner@quizmi.app")
        val outsider = registerAndLogin(email = "outsider.owner@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${outsider.accessToken}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should return not found when course does not exist`() {
        val authentication = registerAndLogin(email = "missing.owner@quizmi.app")

        mockMvc.perform(
            get("/courses/{courseId}", 999_999L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.exception").value("CourseNotFoundException"))
    }

    @Test
    fun `should reject course creation for unauthorized user`() {
        mockMvc.perform(
            post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCourseRequestJson())
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject invalid course request`() {
        val authentication = registerAndLogin(email = "validation.owner@quizmi.app")

        mockMvc.perform(
            post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"name":"ab","description":"short"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.exception").value("MethodArgumentNotValidException"))
    }

    private fun registerAndLogin(email: String): AuthIdentity {
        val password = "password12345678"

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
        )
            .andExpect(status().isCreated)
            .andReturn()

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

    private fun createCourse(
        accessToken: String,
        name: String,
        description: String
    ) {
        mockMvc.perform(
            post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content(createCourseRequestJson(name, description))
        )
            .andExpect(status().isCreated)
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
                .content(createCourseRequestJson(name, description))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return response.readJsonNumber("id")
    }

    private fun createCourseRequestJson(
        name: String = "Spring Security Associate",
        description: String = "A focused course for filters, JWTs, and authorization workflows."
    ): String {
        return """{"name":"$name","description":"$description"}"""
    }

    private fun String.readJsonValue(fieldName: String): String {
        val pattern = """"$fieldName"\s*:\s*\{\s*"value"\s*:\s*"([^"]+)"""".toRegex()
        val match = pattern.find(this)
            ?: throw IllegalStateException("Field $fieldName was not found in response: $this")

        return match.groupValues[1]
    }

    private fun String.readJsonNumber(
        fieldName: String
    ): Long {
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
