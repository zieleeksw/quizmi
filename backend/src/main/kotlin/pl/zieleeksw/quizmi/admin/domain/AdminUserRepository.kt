package pl.zieleeksw.quizmi.admin.domain

import org.springframework.data.jpa.repository.JpaRepository
import pl.zieleeksw.quizmi.user.domain.UserEntity

interface AdminUserRepository : JpaRepository<UserEntity, Long> {
    fun findTop10ByEmailContainingIgnoreCaseOrderByEmailAsc(search: String): List<UserEntity>
}
