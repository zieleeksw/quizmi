package pl.zieleeksw.quizmi.admin.domain

import org.springframework.stereotype.Service
import pl.zieleeksw.quizmi.admin.AdminQuizAttemptDto

@Service
class AdminStatsFacade(
    private val adminQuizAttemptRepository: AdminQuizAttemptRepository
) {

    fun fetchAllQuizAttempts(): List<AdminQuizAttemptDto> {
        return adminQuizAttemptRepository.findAllForAdminConsole()
    }
}
