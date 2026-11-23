package pl.zieleeksw.quizmi.admin.domain

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import pl.zieleeksw.quizmi.admin.AdminCourseOptionDto
import pl.zieleeksw.quizmi.admin.AdminQuizAttemptPageDto
import pl.zieleeksw.quizmi.admin.AdminQuizOptionDto
import pl.zieleeksw.quizmi.admin.AdminUserOptionDto

@Service
class AdminStatsFacade(
    private val adminQuizAttemptRepository: AdminQuizAttemptRepository,
    private val adminUserRepository: AdminUserRepository
) {

    fun fetchAllQuizAttempts(
        page: Int,
        size: Int,
        courseId: Long?,
        quizId: Long?,
        userId: Long?
    ): AdminQuizAttemptPageDto {
        val normalizedPage = page.coerceAtLeast(0)
        val normalizedSize = size.coerceIn(1, MAX_PAGE_SIZE)

        val result = adminQuizAttemptRepository.findAllForAdminConsole(
            courseId,
            quizId,
            userId,
            PageRequest.of(normalizedPage, normalizedSize)
        )

        return AdminQuizAttemptPageDto(
            items = result.content,
            pageNumber = result.number,
            pageSize = result.size,
            totalItems = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            hasPrevious = result.hasPrevious()
        )
    }

    fun fetchCourseOptions(userId: Long?): List<AdminCourseOptionDto> {
        return adminQuizAttemptRepository.findDistinctCourseOptions(userId)
    }

    fun fetchQuizOptions(userId: Long?, courseId: Long?): List<AdminQuizOptionDto> {
        return adminQuizAttemptRepository.findDistinctQuizOptions(userId, courseId)
    }

    fun searchUsers(search: String): List<AdminUserOptionDto> {
        val normalizedSearch = search.trim()

        if (normalizedSearch.length < MIN_SEARCH_LENGTH) {
            return emptyList()
        }

        return adminUserRepository.findTop10ByEmailContainingIgnoreCaseOrderByEmailAsc(normalizedSearch)
            .map { user -> AdminUserOptionDto(userId = requireNotNull(user.id), email = user.email) }
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private const val MIN_SEARCH_LENGTH = 2
    }
}
