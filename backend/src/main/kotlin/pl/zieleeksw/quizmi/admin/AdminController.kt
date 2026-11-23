package pl.zieleeksw.quizmi.admin

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.admin.domain.AdminStatsFacade

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminStatsFacade: AdminStatsFacade
) {

    @GetMapping("/quiz-attempts")
    fun fetchAllQuizAttempts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) courseId: Long?,
        @RequestParam(required = false) quizId: Long?,
        @RequestParam(required = false) userId: Long?
    ): AdminQuizAttemptPageDto {
        return adminStatsFacade.fetchAllQuizAttempts(page, size, courseId, quizId, userId)
    }

    @GetMapping("/courses")
    fun fetchCourseOptions(@RequestParam(required = false) userId: Long?): List<AdminCourseOptionDto> {
        return adminStatsFacade.fetchCourseOptions(userId)
    }

    @GetMapping("/quizzes")
    fun fetchQuizOptions(
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) courseId: Long?
    ): List<AdminQuizOptionDto> {
        return adminStatsFacade.fetchQuizOptions(userId, courseId)
    }

    @GetMapping("/users")
    fun searchUsers(@RequestParam search: String): List<AdminUserOptionDto> {
        return adminStatsFacade.searchUsers(search)
    }
}
