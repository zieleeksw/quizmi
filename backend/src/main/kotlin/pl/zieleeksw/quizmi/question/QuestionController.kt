package pl.zieleeksw.quizmi.question

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.question.domain.QuestionFacade
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@RestController
@RequestMapping("/courses/{courseId}/questions")
class QuestionController(
    private val questionFacade: QuestionFacade,
    private val userFacade: UserFacade
) {

    @GetMapping
    fun fetchAll(
        authentication: Authentication,
        @PathVariable courseId: Long
    ): List<QuestionDto> {
        val currentUser = currentUser(authentication)
        return questionFacade.fetchQuestionListing(courseId, currentUser.id)
    }

    @GetMapping("/preview")
    fun fetchPreview(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) categoryId: Long?
    ): QuestionPageDto {
        val currentUser = currentUser(authentication)
        return questionFacade.fetchQuestionPreview(
            courseId = courseId,
            actorUserId = currentUser.id,
            page = page,
            size = size,
            search = search,
            categoryId = categoryId
        )
    }

    @GetMapping("/{questionId}")
    fun fetchById(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable questionId: Long
    ): QuestionDto {
        val currentUser = currentUser(authentication)
        return questionFacade.fetchQuestionForCourse(courseId, questionId, currentUser.id)
    }

    @GetMapping("/{questionId}/versions")
    fun fetchVersions(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable questionId: Long
    ): List<QuestionVersionDto> {
        val currentUser = currentUser(authentication)
        return questionFacade.fetchQuestionVersions(courseId, questionId, currentUser.id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @RequestBody @Valid request: CreateQuestionRequest
    ): QuestionDto {
        val currentUser = currentUser(authentication)
        return questionFacade.createQuestion(
            courseId = courseId,
            prompt = request.prompt ?: throw IllegalArgumentException("Question prompt cannot be empty."),
            explanation = request.explanation,
            answers = request.answers ?: throw IllegalArgumentException("Question must contain at least 2 answers."),
            categoryIds = request.categoryIds ?: throw IllegalArgumentException("Question must contain at least 1 category."),
            actorUserId = currentUser.id
        )
    }

    @PutMapping("/{questionId}")
    fun update(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable questionId: Long,
        @RequestBody @Valid request: UpdateQuestionRequest
    ): QuestionDto {
        val currentUser = currentUser(authentication)
        return questionFacade.updateQuestion(
            courseId = courseId,
            questionId = questionId,
            prompt = request.prompt ?: throw IllegalArgumentException("Question prompt cannot be empty."),
            explanation = request.explanation,
            answers = request.answers ?: throw IllegalArgumentException("Question must contain at least 2 answers."),
            categoryIds = request.categoryIds ?: throw IllegalArgumentException("Question must contain at least 1 category."),
            actorUserId = currentUser.id
        )
    }

    private fun currentUser(authentication: Authentication): UserDto {
        return userFacade.findUserByEmailOrThrow(authentication.name)
    }
}
