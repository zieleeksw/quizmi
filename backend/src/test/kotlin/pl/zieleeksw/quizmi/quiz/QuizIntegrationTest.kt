package pl.zieleeksw.quizmi.quiz

import com.fasterxml.jackson.databind.ObjectMapper
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuizIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create manual quiz for course owner`() {
        val authentication = registerAndLogin("quiz.owner@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionId = createQuestionAndReadId(courseId, authentication.accessToken, categoryId)

        mockMvc.perform(
            post("/courses/{courseId}/quizzes", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"title":"Authentication mastery","mode":"manual","randomCount":null,"questionOrder":"fixed","answerOrder":"random","questionIds":[$questionId],"categoryIds":[]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Authentication mastery"))
            .andExpect(jsonPath("$.mode").value("manual"))
            .andExpect(jsonPath("$.questionIds[0]").value(questionId))
            .andExpect(jsonPath("$.resolvedQuestionCount").value(1))
    }

    @Test
    fun `should score multi answer quiz only on exact correct set`() {
        val authentication = registerAndLogin("quiz.attempt@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionResponse = createQuestionAndReadResponse(courseId, authentication.accessToken, categoryId)
        val questionId = questionResponse["id"].asLong()
        val answers = questionResponse["answers"]
        val firstCorrectId = answers.first { it["content"].asText() == "Refresh token flow" }["id"].asLong()
        val secondCorrectId = answers.first { it["content"].asText() == "Browser session binding" }["id"].asLong()
        val wrongId = answers.first { it["content"].asText() == "Static file serving" }["id"].asLong()
        val quizId = createQuizAndReadId(courseId, authentication.accessToken, questionId)

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"answers":[{"questionId":$questionId,"answerIds":[$firstCorrectId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.correctAnswers").value(0))

        val exactAttemptResponse = mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"answers":[{"questionId":$questionId,"answerIds":[$firstCorrectId,$secondCorrectId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.correctAnswers").value(1))
            .andReturn()
            .response
            .contentAsString

        val attemptId = objectMapper.readTree(exactAttemptResponse)["id"].asLong()

        mockMvc.perform(
            get("/courses/{courseId}/attempts/{attemptId}", courseId, attemptId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.questions[0].answeredCorrectly").value(true))
            .andExpect(jsonPath("$.questions[0].explanation").value("Refresh tokens should be protected by session-aware controls and never replaced by unrelated infrastructure concerns."))
            .andExpect(jsonPath("$.questions[0].selectedAnswerIds.length()").value(2))

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"answers":[{"questionId":$questionId,"answerIds":[$firstCorrectId,$secondCorrectId,$wrongId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.correctAnswers").value(0))
    }

    @Test
    fun `should limit quiz preview for user without course access`() {
        val owner = registerAndLogin("quiz.shared.owner@quizmi.app")
        val viewer = registerAndLogin("quiz.shared.viewer@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")

        (1..4).forEach { index ->
            val questionId = createQuestionAndReadId(courseId, owner.accessToken, categoryId, "Which quiz preview question $index belongs to the course?")
            createQuizAndReadId(courseId, owner.accessToken, questionId, "Quiz Preview $index")
        }

        mockMvc.perform(
            get("/courses/{courseId}/quizzes", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `should list quizzes with correct resolved counts for mixed quiz modes`() {
        val authentication = registerAndLogin("quiz.listing.performance@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val authenticationCategoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val authorizationCategoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authorization")
        val firstQuestionId = createQuestionAndReadId(courseId, authentication.accessToken, authenticationCategoryId)
        createQuestionAndReadId(
            courseId = courseId,
            accessToken = authentication.accessToken,
            categoryId = authenticationCategoryId,
            prompt = "Which controls improve refresh token replay resistance?"
        )
        createQuestionAndReadId(
            courseId = courseId,
            accessToken = authentication.accessToken,
            categoryId = authorizationCategoryId,
            prompt = "Which checks keep authorization boundaries explicit?"
        )

        createQuizAndReadId(courseId, authentication.accessToken, firstQuestionId, "Manual listing quiz")
        createQuizAndReadResponse(
            courseId = courseId,
            accessToken = authentication.accessToken,
            requestJson = """
                {"title":"Random listing quiz","mode":"random","randomCount":2,"questionOrder":"random","answerOrder":"fixed","questionIds":[],"categoryIds":[]}
            """.trimIndent()
        )
        createQuizAndReadResponse(
            courseId = courseId,
            accessToken = authentication.accessToken,
            requestJson = """
                {"title":"Category listing quiz","mode":"category","randomCount":5,"questionOrder":"fixed","answerOrder":"random","questionIds":[],"categoryIds":[$authenticationCategoryId]}
            """.trimIndent()
        )

        val response = mockMvc.perform(
            get("/courses/{courseId}/quizzes", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andReturn()
            .response
            .contentAsString

        val quizzesByTitle = objectMapper.readTree(response).associateBy { it["title"].asText() }
        val manualQuiz = quizzesByTitle["Manual listing quiz"]
            ?: throw AssertionError("Manual listing quiz should be present in the response.")
        val randomQuiz = quizzesByTitle["Random listing quiz"]
            ?: throw AssertionError("Random listing quiz should be present in the response.")
        val categoryQuiz = quizzesByTitle["Category listing quiz"]
            ?: throw AssertionError("Category listing quiz should be present in the response.")

        assertEquals(1, manualQuiz["resolvedQuestionCount"].asInt())
        assertEquals(firstQuestionId, manualQuiz["questionIds"][0].asLong())
        assertEquals(2, randomQuiz["resolvedQuestionCount"].asInt())
        assertEquals(2, categoryQuiz["resolvedQuestionCount"].asInt())
        assertEquals(authenticationCategoryId, categoryQuiz["categories"][0]["id"].asLong())
    }

    @Test
    fun `should allow active course member to browse quiz and review own statistics`() {
        val owner = registerAndLogin("quiz.member.owner@quizmi.app")
        val viewer = registerAndLogin("quiz.member.viewer@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")
        val questionResponse = createQuestionAndReadResponse(courseId, owner.accessToken, categoryId)
        val questionId = questionResponse["id"].asLong()
        val correctAnswerIds = questionResponse["answers"]
            .filter { it["correct"].asBoolean() }
            .map { it["id"].asLong() }
        val quizId = createQuizAndReadId(courseId, owner.accessToken, questionId)

        requestAndApproveCourseJoin(courseId, viewer, owner)

        mockMvc.perform(
            get("/courses/{courseId}/quizzes/{quizId}", courseId, quizId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(quizId))
            .andExpect(jsonPath("$.resolvedQuestionCount").value(1))

        val attemptResponse = mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
                .content(
                    """
                    {"answers":[{"questionId":$questionId,"answerIds":[${correctAnswerIds.joinToString(",")}]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.correctAnswers").value(1))
            .andReturn()
            .response
            .contentAsString

        val attemptId = objectMapper.readTree(attemptResponse)["id"].asLong()

        mockMvc.perform(
            get("/courses/{courseId}/attempts", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(attemptId))

        mockMvc.perform(
            get("/courses/{courseId}/attempts/reviews", courseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].quizId").value(quizId))
            .andExpect(jsonPath("$[0].questions[0].explanation").value("Refresh tokens should be protected by session-aware controls and never replaced by unrelated infrastructure concerns."))

        mockMvc.perform(
            get("/courses/{courseId}/attempts/{attemptId}", courseId, attemptId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(attemptId))
            .andExpect(jsonPath("$.questions[0].answeredCorrectly").value(true))
            .andExpect(jsonPath("$.questions[0].explanation").value("Refresh tokens should be protected by session-aware controls and never replaced by unrelated infrastructure concerns."))
    }

    @Test
    fun `should allow moderator to create quiz`() {
        val owner = registerAndLogin("quiz.moderator.owner@quizmi.app")
        val moderator = registerAndLogin("quiz.moderator.member@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")
        val questionId = createQuestionAndReadId(courseId, owner.accessToken, categoryId)

        requestAndApproveCourseJoin(courseId, moderator, owner)
        promoteToModerator(courseId, moderator, owner)

        mockMvc.perform(
            post("/courses/{courseId}/quizzes", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${moderator.accessToken}")
                .content(
                    """
                    {"title":"Authentication mastery","mode":"manual","randomCount":null,"questionOrder":"fixed","answerOrder":"random","questionIds":[$questionId],"categoryIds":[]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Authentication mastery"))
            .andExpect(jsonPath("$.questionIds[0]").value(questionId))
    }

    @Test
    fun `should allow finishing quiz attempt with skipped question`() {
        val authentication = registerAndLogin("quiz.skip@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionId = createQuestionAndReadId(courseId, authentication.accessToken, categoryId)
        val quizId = createQuizAndReadId(courseId, authentication.accessToken, questionId)

        val attemptResponse = mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"answers":[]}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.correctAnswers").value(0))
            .andExpect(jsonPath("$.totalQuestions").value(1))
            .andReturn()
            .response
            .contentAsString

        val attemptId = objectMapper.readTree(attemptResponse)["id"].asLong()

        mockMvc.perform(
            get("/courses/{courseId}/attempts/{attemptId}", courseId, attemptId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.questions[0].questionId").value(questionId))
            .andExpect(jsonPath("$.questions[0].selectedAnswerIds.length()").value(0))
            .andExpect(jsonPath("$.questions[0].answeredCorrectly").value(false))
    }

    @Test
    fun `should clear stale session answers after question update and still finish quiz`() {
        val authentication = registerAndLogin("quiz.stale.answers@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionResponse = createQuestionAndReadResponse(courseId, authentication.accessToken, categoryId)
        val questionId = questionResponse["id"].asLong()
        val staleAnswerId = questionResponse["answers"][0]["id"].asLong()
        val quizId = createQuizAndReadId(courseId, authentication.accessToken, questionId)

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{}""")
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":0,"answers":[{"questionId":$questionId,"answerIds":[$staleAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.answers['$questionId'][0]").value(staleAnswerId))

        mockMvc.perform(
            put("/courses/{courseId}/questions/{questionId}", courseId, questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"prompt":"Which updated controls should remain true for a secure token refresh flow?","explanation":"Updated explanation should not break active quiz sessions.","answers":[{"content":"Refresh token rotation","correct":true},{"content":"Browser session binding","correct":true},{"content":"Static file serving","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.answers").isEmpty())

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":0,"answers":[{"questionId":$questionId,"answerIds":[$staleAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.answers").isEmpty())

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"answers":[{"questionId":$questionId,"answerIds":[$staleAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.correctAnswers").value(0))
            .andExpect(jsonPath("$.totalQuestions").value(1))
    }

    @Test
    fun `should reject editing answered previous session question`() {
        val authentication = registerAndLogin("quiz.locked.previous@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val firstQuestionResponse = createQuestionAndReadResponse(
            courseId,
            authentication.accessToken,
            categoryId,
            "Which controls should lock after moving to the next quiz question?"
        )
        val secondQuestionResponse = createQuestionAndReadResponse(
            courseId,
            authentication.accessToken,
            categoryId,
            "Which follow up question keeps the quiz session moving forward?"
        )
        val firstQuestionId = firstQuestionResponse["id"].asLong()
        val secondQuestionId = secondQuestionResponse["id"].asLong()
        val persistedAnswerId = firstQuestionResponse["answers"][0]["id"].asLong()
        val replacementAnswerId = firstQuestionResponse["answers"][2]["id"].asLong()
        val quizId = createQuizAndReadResponse(
            courseId = courseId,
            accessToken = authentication.accessToken,
            requestJson = """
                {"title":"Locked previous answers","mode":"manual","randomCount":null,"questionOrder":"fixed","answerOrder":"fixed","questionIds":[$firstQuestionId,$secondQuestionId],"categoryIds":[]}
            """.trimIndent()
        ).readJsonNumber("id")

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentIndex").value(0))
            .andExpect(jsonPath("$.furthestIndex").value(0))

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":1,"answers":[{"questionId":$firstQuestionId,"answerIds":[$persistedAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentIndex").value(1))
            .andExpect(jsonPath("$.furthestIndex").value(1))
            .andExpect(jsonPath("$.answers['$firstQuestionId'][0]").value(persistedAnswerId))

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":0,"answers":[{"questionId":$firstQuestionId,"answerIds":[$persistedAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentIndex").value(0))
            .andExpect(jsonPath("$.furthestIndex").value(1))
            .andExpect(jsonPath("$.answers['$firstQuestionId'][0]").value(persistedAnswerId))

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":0,"answers":[{"questionId":$firstQuestionId,"answerIds":[$replacementAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject editing checked current session question after resume`() {
        val authentication = registerAndLogin("quiz.locked.checked@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionResponse = createQuestionAndReadResponse(courseId, authentication.accessToken, categoryId)
        val questionId = questionResponse["id"].asLong()
        val persistedAnswerId = questionResponse["answers"][2]["id"].asLong()
        val replacementAnswerId = questionResponse["answers"][0]["id"].asLong()
        val quizId = createQuizAndReadId(courseId, authentication.accessToken, questionId)

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{}""")
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":0,"checkedQuestionIds":[$questionId],"answers":[{"questionId":$questionId,"answerIds":[$persistedAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentIndex").value(0))
            .andExpect(jsonPath("$.furthestIndex").value(0))
            .andExpect(jsonPath("$.checkedQuestionIds[0]").value(questionId))
            .andExpect(jsonPath("$.answers['$questionId'][0]").value(persistedAnswerId))

        mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.checkedQuestionIds[0]").value(questionId))

        mockMvc.perform(
            put("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content(
                    """
                    {"currentIndex":0,"checkedQuestionIds":[$questionId],"answers":[{"questionId":$questionId,"answerIds":[$replacementAnswerId]}]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should persist randomized answer order across session resume and review`() {
        val authentication = registerAndLogin("quiz.answer.order@quizmi.app")
        val courseId = createCourseAndReadId(authentication.accessToken)
        val categoryId = createCategoryAndReadId(courseId, authentication.accessToken, "Authentication")
        val questionResponse = createQuestionWithAnswersAndReadResponse(
            courseId = courseId,
            accessToken = authentication.accessToken,
            categoryId = categoryId,
            prompt = "Which controls improve the resilience of a token refresh flow?",
            answersJson = """
                [
                  {"content":"Refresh token rotation","correct":true},
                  {"content":"Browser session binding","correct":true},
                  {"content":"Static file serving","correct":false},
                  {"content":"Rate limiting","correct":false},
                  {"content":"Revocation tracking","correct":true},
                  {"content":"Image optimization","correct":false}
                ]
            """.trimIndent()
        )
        val questionId = questionResponse["id"].asLong()
        val expectedAnswerIds = questionResponse["answers"].map { it["id"].asLong() }.toSet()
        val quizId = createQuizAndReadId(courseId, authentication.accessToken, questionId)

        val firstSession = objectMapper.readTree(
            mockMvc.perform(
                post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                    .content("""{}""")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )
        val firstOrder = firstSession.readAnswerOrder(questionId)

        assertEquals(expectedAnswerIds, firstOrder.toSet())

        val resumedSession = objectMapper.readTree(
            mockMvc.perform(
                post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                    .content("""{}""")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )

        assertEquals(firstOrder, resumedSession.readAnswerOrder(questionId))

        val attemptResponse = mockMvc.perform(
            post("/courses/{courseId}/quizzes/{quizId}/attempts", courseId, quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
                .content("""{"answers":[]}""")
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val attemptId = objectMapper.readTree(attemptResponse)["id"].asLong()
        val review = objectMapper.readTree(
            mockMvc.perform(
                get("/courses/{courseId}/attempts/{attemptId}", courseId, attemptId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${authentication.accessToken}")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )
        val reviewAnswerOrder = review["questions"][0]["answers"].map { it["id"].asLong() }

        assertEquals(firstOrder, reviewAnswerOrder)
    }

    @Test
    fun `should keep randomized answer order stable per user session and different from base order`() {
        val owner = registerAndLogin("quiz.answer.order.owner@quizmi.app")
        val viewer = registerAndLogin("quiz.answer.order.viewer@quizmi.app")
        val courseId = createCourseAndReadId(owner.accessToken)
        val categoryId = createCategoryAndReadId(courseId, owner.accessToken, "Authentication")
        val questionResponse = createQuestionWithAnswersAndReadResponse(
            courseId = courseId,
            accessToken = owner.accessToken,
            categoryId = categoryId,
            prompt = "Which hardening steps can support a token refresh pipeline?",
            answersJson = """
                [
                  {"content":"Refresh token rotation","correct":true},
                  {"content":"Browser session binding","correct":true},
                  {"content":"Revocation tracking","correct":true},
                  {"content":"Rate limiting","correct":false},
                  {"content":"Static file serving","correct":false},
                  {"content":"Image optimization","correct":false}
                ]
            """.trimIndent()
        )
        val questionId = questionResponse["id"].asLong()
        val baseOrder = questionResponse["answers"].map { it["id"].asLong() }
        val quizId = createQuizAndReadId(courseId, owner.accessToken, questionId)

        requestAndApproveCourseJoin(courseId, viewer, owner)

        val ownerSession = objectMapper.readTree(
            mockMvc.perform(
                post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
                    .content("""{}""")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )
        val ownerResumedSession = objectMapper.readTree(
            mockMvc.perform(
                post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${owner.accessToken}")
                    .content("""{}""")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )
        val viewerSession = objectMapper.readTree(
            mockMvc.perform(
                post("/courses/{courseId}/quizzes/{quizId}/session", courseId, quizId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${viewer.accessToken}")
                    .content("""{}""")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )

        val ownerOrder = ownerSession.readAnswerOrder(questionId)
        val ownerResumedOrder = ownerResumedSession.readAnswerOrder(questionId)
        val viewerOrder = viewerSession.readAnswerOrder(questionId)

        assertEquals(6, ownerOrder.size)
        assertEquals(baseOrder.toSet(), ownerOrder.toSet())
        assertEquals(ownerOrder, ownerResumedOrder)
        assertTrue(ownerOrder != baseOrder, "Random answer order should differ from the base display order for the owner session.")
        assertEquals(6, viewerOrder.size)
        assertEquals(baseOrder.toSet(), viewerOrder.toSet())
        assertTrue(viewerOrder != baseOrder, "Random answer order should differ from the base display order for the viewer session.")
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

    private fun createQuestionAndReadId(
        courseId: Long,
        accessToken: String,
        categoryId: Long,
        prompt: String = "Which controls should remain true for a secure token refresh flow?"
    ): Long {
        return createQuestionAndReadResponse(courseId, accessToken, categoryId, prompt)["id"].asLong()
    }

    private fun createQuestionAndReadResponse(
        courseId: Long,
        accessToken: String,
        categoryId: Long,
        prompt: String = "Which controls should remain true for a secure token refresh flow?"
    ) = objectMapper.readTree(
        mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content(
                    """
                    {"prompt":"$prompt","explanation":"Refresh tokens should be protected by session-aware controls and never replaced by unrelated infrastructure concerns.","answers":[{"content":"Refresh token flow","correct":true},{"content":"Browser session binding","correct":true},{"content":"Static file serving","correct":false}],"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
    )

    private fun createQuestionWithAnswersAndReadResponse(
        courseId: Long,
        accessToken: String,
        categoryId: Long,
        prompt: String,
        answersJson: String
    ) = objectMapper.readTree(
        mockMvc.perform(
            post("/courses/{courseId}/questions", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content(
                    """
                    {"prompt":"$prompt","explanation":"Answer ordering should stay consistent for the whole quiz attempt.","answers":$answersJson,"categoryIds":[$categoryId]}
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
    )

    private fun createQuizAndReadId(
        courseId: Long,
        accessToken: String,
        questionId: Long,
        title: String = "Authentication mastery"
    ): Long {
        val response = createQuizAndReadResponse(
            courseId = courseId,
            accessToken = accessToken,
            requestJson = """
                {"title":"$title","mode":"manual","randomCount":null,"questionOrder":"fixed","answerOrder":"random","questionIds":[$questionId],"categoryIds":[]}
            """.trimIndent()
        )

        return response.readJsonNumber("id")
    }

    private fun createQuizAndReadResponse(
        courseId: Long,
        accessToken: String,
        requestJson: String
    ): String {
        return mockMvc.perform(
            post("/courses/{courseId}/quizzes", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
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

    private fun com.fasterxml.jackson.databind.JsonNode.readAnswerOrder(questionId: Long): List<Long> {
        val answerOrderNode = this["answerOrderByQuestion"][questionId.toString()]
        assertTrue(answerOrderNode != null && answerOrderNode.isArray, "Expected answerOrderByQuestion for question $questionId.")
        return answerOrderNode.map { it.asLong() }
    }
}
