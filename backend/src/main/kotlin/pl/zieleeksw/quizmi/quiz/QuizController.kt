package pl.zieleeksw.quizmi.quiz

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.quiz.domain.QuizFacade
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@RestController
@RequestMapping("/courses/{courseId}/quizzes")
class QuizController(
    private val quizFacade: QuizFacade,
    private val userFacade: UserFacade
) {

    @GetMapping
    fun fetchAll(authentication: Authentication, @PathVariable courseId: Long): List<QuizDto> {
        val currentUser = currentUser(authentication)
        return quizFacade.fetchQuizzes(courseId, currentUser.id)
    }

    @GetMapping("/{quizId}")
    fun fetchById(authentication: Authentication, @PathVariable courseId: Long, @PathVariable quizId: Long): QuizDto {
        val currentUser = currentUser(authentication)
        return quizFacade.fetchQuizForCourse(courseId, quizId, currentUser.id)
    }

    @GetMapping("/{quizId}/versions")
    fun fetchVersions(authentication: Authentication, @PathVariable courseId: Long, @PathVariable quizId: Long): List<QuizVersionDto> {
        val currentUser = currentUser(authentication)
        return quizFacade.fetchQuizVersions(courseId, quizId, currentUser.id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(authentication: Authentication, @PathVariable courseId: Long, @RequestBody request: CreateQuizRequest): QuizDto {
        val currentUser = currentUser(authentication)
        return quizFacade.createQuiz(
            courseId = courseId,
            title = request.title ?: throw IllegalArgumentException("Quiz title is required."),
            mode = request.mode ?: throw IllegalArgumentException("Quiz mode is required."),
            randomCount = request.randomCount,
            questionOrder = request.questionOrder ?: throw IllegalArgumentException("Question order is required."),
            answerOrder = request.answerOrder ?: throw IllegalArgumentException("Answer order is required."),
            questionIds = request.questionIds ?: emptyList(),
            categoryIds = request.categoryIds ?: emptyList(),
            actorUserId = currentUser.id
        )
    }

    @PutMapping("/{quizId}")
    fun update(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable quizId: Long,
        @RequestBody request: UpdateQuizRequest
    ): QuizDto {
        val currentUser = currentUser(authentication)
        return quizFacade.updateQuiz(
            courseId = courseId,
            quizId = quizId,
            title = request.title ?: throw IllegalArgumentException("Quiz title is required."),
            mode = request.mode ?: throw IllegalArgumentException("Quiz mode is required."),
            randomCount = request.randomCount,
            questionOrder = request.questionOrder ?: throw IllegalArgumentException("Question order is required."),
            answerOrder = request.answerOrder ?: throw IllegalArgumentException("Answer order is required."),
            questionIds = request.questionIds ?: emptyList(),
            categoryIds = request.categoryIds ?: emptyList(),
            actorUserId = currentUser.id
        )
    }

    @DeleteMapping("/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(authentication: Authentication, @PathVariable courseId: Long, @PathVariable quizId: Long) {
        val currentUser = currentUser(authentication)
        quizFacade.deleteQuiz(courseId, quizId, currentUser.id)
    }

    private fun currentUser(authentication: Authentication): UserDto {
        return userFacade.findUserByEmailOrThrow(authentication.name)
    }
}
