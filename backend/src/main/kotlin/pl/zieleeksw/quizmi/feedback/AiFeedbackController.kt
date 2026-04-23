package pl.zieleeksw.quizmi.feedback

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade


@RestController
@RequestMapping("/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/feedback")
class AiFeedbackController(
    private val aiFeedbackFacade: AiFeedbackFacade,
    private val userFacade: UserFacade
) {

    @PostMapping
    fun generateFeedback(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable quizId: Long,
        @PathVariable questionId: Long,
        @RequestBody request: AiFeedbackRequest
    ): AiFeedbackDto {
        val currentUser = currentUser(authentication)
        return aiFeedbackFacade.generateFeedback(
            courseId = courseId,
            quizId = quizId,
            questionId = questionId,
            userId = currentUser.id,
            selectedAnswerIds = request.selectedAnswerIds.orEmpty()
        )
    }

    private fun currentUser(authentication: Authentication): UserDto {
        return userFacade.findUserByEmailOrThrow(authentication.name)
    }
}
