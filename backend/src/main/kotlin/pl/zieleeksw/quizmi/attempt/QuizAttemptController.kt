package pl.zieleeksw.quizmi.attempt

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.attempt.domain.QuizAttemptFacade
import pl.zieleeksw.quizmi.attempt.domain.QuizSessionFacade
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@RestController
@RequestMapping("/courses/{courseId}")
class QuizAttemptController(
    private val quizSessionFacade: QuizSessionFacade,
    private val quizAttemptFacade: QuizAttemptFacade,
    private val userFacade: UserFacade
) {

    @GetMapping("/sessions")
    fun fetchSessions(authentication: Authentication, @PathVariable courseId: Long): List<QuizSessionDto> {
        val currentUser = currentUser(authentication)
        return quizSessionFacade.fetchCourseSessions(courseId, currentUser.id)
    }

    @PostMapping("/quizzes/{quizId}/session")
    fun createOrResumeSession(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable quizId: Long
    ): QuizSessionDto {
        val currentUser = currentUser(authentication)
        return quizSessionFacade.createOrResumeSession(courseId, quizId, currentUser.id)
    }

    @PutMapping("/quizzes/{quizId}/session")
    fun updateSession(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable quizId: Long,
        @RequestBody request: QuizSessionUpdateRequest
    ): QuizSessionDto {
        val currentUser = currentUser(authentication)
        return quizSessionFacade.updateSession(
            courseId = courseId,
            quizId = quizId,
            userId = currentUser.id,
            currentIndex = request.currentIndex ?: 0,
            answers = request.answers ?: emptyList()
        )
    }

    @GetMapping("/attempts")
    fun fetchAttempts(authentication: Authentication, @PathVariable courseId: Long): List<QuizAttemptDto> {
        val currentUser = currentUser(authentication)
        return quizAttemptFacade.fetchCourseAttempts(courseId, currentUser.id)
    }

    @GetMapping("/attempts/reviews")
    fun fetchAttemptReviews(authentication: Authentication, @PathVariable courseId: Long): List<QuizAttemptDetailDto> {
        val currentUser = currentUser(authentication)
        return quizAttemptFacade.fetchCourseAttemptReviews(courseId, currentUser.id)
    }

    @GetMapping("/attempts/{attemptId}")
    fun fetchAttemptDetail(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable attemptId: Long
    ): QuizAttemptDetailDto {
        val currentUser = currentUser(authentication)
        return quizAttemptFacade.fetchAttemptDetail(courseId, attemptId, currentUser.id)
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAttempt(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable quizId: Long,
        @RequestBody request: CreateQuizAttemptRequest
    ): QuizAttemptDto {
        val currentUser = currentUser(authentication)
        return quizAttemptFacade.createAttempt(
            courseId = courseId,
            quizId = quizId,
            userId = currentUser.id,
            answers = request.answers ?: emptyList()
        )
    }

    private fun currentUser(authentication: Authentication): UserDto {
        return userFacade.findUserByEmailOrThrow(authentication.name)
    }
}
