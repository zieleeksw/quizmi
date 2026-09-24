package pl.zieleeksw.quizmi.admin

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.admin.domain.AdminStatsFacade

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminStatsFacade: AdminStatsFacade
) {

    @GetMapping("/quiz-attempts")
    fun fetchAllQuizAttempts(): List<AdminQuizAttemptDto> {
        return adminStatsFacade.fetchAllQuizAttempts()
    }
}
