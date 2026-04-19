package pl.zieleeksw.quizmi.course

import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
            .andExpect(jsonPath("$.ownerEmail").value("course.owner@quizmi.app"))
            .andExpect(jsonPath("$.membershipRole").value("OWNER"))
            .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.canAccess").value(true))
            .andExpect(jsonPath("$.canManage").value(true))
    }

    @Test
    fun `should fetch all visible courses for authenticated user`() {
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
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("Spring Security Associate"))
            .andExpect(jsonPath("$[0].ownerEmail").value("first.owner@quizmi.app"))
            .andExpect(jsonPath("$[1].name").value("Docker Basics"))
            .andExpect(jsonPath("$[1].ownerEmail").value("second.owner@quizmi.app"))
            .andExpect(jsonPath("$[2].name").value("Spring Boot Associate"))
            .andExpect(jsonPath("$[0].ownerUserId").value(firstUser.userId))
            .andExpect(jsonPath("$[1].ownerUserId").value(secondUser.userId))
            .andExpect(jsonPath("$[2].ownerUserId").value(firstUser.userId))
            .andExpect(jsonPath("$[0].membershipRole").value("OWNER"))
            .andExpect(jsonPath("$[1].membershipRole").value(nullValue()))
            .andExpect(jsonPath("$[1].canAccess").value(false))
    }

    @Test
    fun `should fetch course details for any authenticated user`() {
        val authentication = registerAndLogin(email = "details.owner@quizmi.app")
        val outsider = registerAndLogin(email = "details.viewer@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = authentication.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${outsider.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(courseId))
            .andExpect(jsonPath("$.name").value("Spring Security Associate"))
            .andExpect(jsonPath("$.description").value("A focused course for filters, JWTs, and authorization workflows."))
            .andExpect(jsonPath("$.ownerUserId").value(authentication.userId))
            .andExpect(jsonPath("$.ownerEmail").value("details.owner@quizmi.app"))
            .andExpect(jsonPath("$.canAccess").value(false))
            .andExpect(jsonPath("$.membershipStatus").value(nullValue()))
    }

    @Test
    fun `should create join request and allow owner to approve it`() {
        val owner = registerAndLogin(email = "course.members.owner@quizmi.app")
        val requester = registerAndLogin(email = "course.members.requester@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            post("/courses/{courseId}/join-requests", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(courseId))
            .andExpect(jsonPath("$.membershipRole").value("MEMBER"))
            .andExpect(jsonPath("$.membershipStatus").value("PENDING"))
            .andExpect(jsonPath("$.canAccess").value(false))

        mockMvc.perform(
            get("/courses/{courseId}/members", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.members.length()").value(1))
            .andExpect(jsonPath("$.members[0].role").value("OWNER"))
            .andExpect(jsonPath("$.pendingRequests.length()").value(1))
            .andExpect(jsonPath("$.pendingRequests[0].email").value("course.members.requester@quizmi.app"))

        mockMvc.perform(
            post("/courses/{courseId}/members/{memberUserId}/approve", courseId, requester.userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(requester.userId))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.canAccess").value(true))
            .andExpect(jsonPath("$.membershipRole").value("MEMBER"))
            .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
    }

    @Test
    fun `should forbid pending requester from fetching course members`() {
        val owner = registerAndLogin(email = "course.members.pending.owner@quizmi.app")
        val requester = registerAndLogin(email = "course.members.pending.requester@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            post("/courses/{courseId}/join-requests", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            get("/courses/{courseId}/members", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.exception").value("AccessDeniedException"))
    }

    @Test
    fun `should allow owner to promote active member to moderator`() {
        val owner = registerAndLogin(email = "course.roles.owner@quizmi.app")
        val requester = registerAndLogin(email = "course.roles.requester@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        requestAndApproveCourseJoin(courseId, requester, owner)

        mockMvc.perform(
            put("/courses/{courseId}/members/{memberUserId}/role", courseId, requester.userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
                .content("""{"role":"MODERATOR"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(requester.userId))
            .andExpect(jsonPath("$.role").value("MODERATOR"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.membershipRole").value("MODERATOR"))
            .andExpect(jsonPath("$.canManage").value(true))
    }

    @Test
    fun `should allow owner to remove active course member`() {
        val owner = registerAndLogin(email = "course.remove.owner@quizmi.app")
        val member = registerAndLogin(email = "course.remove.member@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        requestAndApproveCourseJoin(courseId, member, owner)

        mockMvc.perform(
            delete("/courses/{courseId}/members/{memberUserId}", courseId, member.userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${member.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.membershipRole").value(nullValue()))
            .andExpect(jsonPath("$.membershipStatus").value(nullValue()))
            .andExpect(jsonPath("$.canAccess").value(false))
    }

    @Test
    fun `should forbid moderator from demoting moderator to member`() {
        val owner = registerAndLogin(email = "course.roles.demote.owner@quizmi.app")
        val moderator = registerAndLogin(email = "course.roles.demote.moderator@quizmi.app")
        val secondModerator = registerAndLogin(email = "course.roles.demote.target@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        requestAndApproveCourseJoin(courseId, moderator, owner)
        requestAndApproveCourseJoin(courseId, secondModerator, owner)
        promoteToModerator(courseId, moderator, owner)
        promoteToModerator(courseId, secondModerator, owner)

        mockMvc.perform(
            put("/courses/{courseId}/members/{memberUserId}/role", courseId, secondModerator.userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${moderator.accessToken}")
                .content("""{"role":"MEMBER"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.exception").value("AccessDeniedException"))
            .andExpect(jsonPath("$.message").value("Access denied."))
    }

    @Test
    fun `should forbid moderator from promoting member to moderator`() {
        val owner = registerAndLogin(email = "course.roles.promote.owner@quizmi.app")
        val moderator = registerAndLogin(email = "course.roles.promote.moderator@quizmi.app")
        val member = registerAndLogin(email = "course.roles.promote.member@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        requestAndApproveCourseJoin(courseId, moderator, owner)
        requestAndApproveCourseJoin(courseId, member, owner)
        promoteToModerator(courseId, moderator, owner)

        mockMvc.perform(
            put("/courses/{courseId}/members/{memberUserId}/role", courseId, member.userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${moderator.accessToken}")
                .content("""{"role":"MODERATOR"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.exception").value("AccessDeniedException"))
    }

    @Test
    fun `should allow moderator to remove active moderator`() {
        val owner = registerAndLogin(email = "course.remove.mod.owner@quizmi.app")
        val moderator = registerAndLogin(email = "course.remove.mod.moderator@quizmi.app")
        val secondModerator = registerAndLogin(email = "course.remove.mod.target@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        requestAndApproveCourseJoin(courseId, moderator, owner)
        requestAndApproveCourseJoin(courseId, secondModerator, owner)
        promoteToModerator(courseId, moderator, owner)
        promoteToModerator(courseId, secondModerator, owner)

        mockMvc.perform(
            delete("/courses/{courseId}/members/{memberUserId}", courseId, secondModerator.userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${moderator.accessToken}")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should allow owner to decline pending join request`() {
        val owner = registerAndLogin(email = "course.decline.owner@quizmi.app")
        val requester = registerAndLogin(email = "course.decline.requester@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            post("/courses/{courseId}/join-requests", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            delete("/courses/{courseId}/members/{memberUserId}/request", courseId, requester.userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/courses/{courseId}/members", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pendingRequests.length()").value(0))

        mockMvc.perform(
            get("/courses/{courseId}", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${requester.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.membershipRole").value(nullValue()))
            .andExpect(jsonPath("$.membershipStatus").value(nullValue()))
            .andExpect(jsonPath("$.canAccess").value(false))
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
    fun `should forbid updating course details for different owner`() {
        val owner = registerAndLogin(email = "private.owner@quizmi.app")
        val outsider = registerAndLogin(email = "outsider.owner@quizmi.app")
        val courseId = createCourseAndReadId(
            accessToken = owner.accessToken,
            name = "Spring Security Associate",
            description = "A focused course for filters, JWTs, and authorization workflows."
        )

        mockMvc.perform(
            put("/courses/{courseId}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${outsider.accessToken}")
                .content(
                    """{"name":"Spring Security Mastery","description":"An updated course for advanced filters, JWTs, and authorization design."}"""
                )
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
