package pl.zieleeksw.quizmi.course

import jakarta.validation.Valid
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
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@RestController
@RequestMapping("/courses")
class CourseController(
    private val courseFacade: CourseFacade,
    private val userFacade: UserFacade
) {

    @GetMapping
    fun fetchAll(authentication: Authentication): List<CourseDto> {
        return courseFacade.fetchVisibleCourses(
            actorUserId = currentUser(authentication).id
        )
    }

    @GetMapping("/{courseId}")
    fun fetchById(
        authentication: Authentication,
        @PathVariable courseId: Long
    ): CourseDto {
        return courseFacade.fetchCourseById(
            id = courseId,
            actorUserId = currentUser(authentication).id
        )
    }

    @PostMapping("/{courseId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestToJoin(
        authentication: Authentication,
        @PathVariable courseId: Long
    ): CourseDto {
        return courseFacade.requestToJoinCourse(
            id = courseId,
            actorUserId = currentUser(authentication).id
        )
    }

    @GetMapping("/{courseId}/members")
    fun fetchMembers(
        authentication: Authentication,
        @PathVariable courseId: Long
    ): CourseMembersDto {
        return courseFacade.fetchCourseMembers(
            id = courseId,
            actorUserId = currentUser(authentication).id
        )
    }

    @PostMapping("/{courseId}/members/{memberUserId}/approve")
    fun approveJoinRequest(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable memberUserId: Long
    ): CourseMemberDto {
        return courseFacade.approveJoinRequest(
            id = courseId,
            memberUserId = memberUserId,
            actorUserId = currentUser(authentication).id
        )
    }

    @DeleteMapping("/{courseId}/members/{memberUserId}/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun declineJoinRequest(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable memberUserId: Long
    ) {
        courseFacade.declineJoinRequest(
            id = courseId,
            memberUserId = memberUserId,
            actorUserId = currentUser(authentication).id
        )
    }

    @DeleteMapping("/{courseId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable memberUserId: Long
    ) {
        courseFacade.removeCourseMember(
            id = courseId,
            memberUserId = memberUserId,
            actorUserId = currentUser(authentication).id
        )
    }

    @PutMapping("/{courseId}/members/{memberUserId}/role")
    fun updateMemberRole(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable memberUserId: Long,
        @RequestBody request: UpdateCourseMemberRoleRequest
    ): CourseMemberDto {
        val role = request.role ?: throw IllegalArgumentException("Course member role is required.")

        return courseFacade.updateCourseMemberRole(
            id = courseId,
            memberUserId = memberUserId,
            role = role,
            actorUserId = currentUser(authentication).id
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: Authentication,
        @RequestBody @Valid request: CreateCourseRequest
    ): CourseDto {
        val currentUser = currentUser(authentication)
        val name = request.name ?: throw IllegalArgumentException("Course name cannot be empty.")
        val description = request.description ?: throw IllegalArgumentException("Course description cannot be empty.")

        return courseFacade.createCourse(
            name = name,
            description = description,
            ownerUserId = currentUser.id
        )
    }

    @PutMapping("/{courseId}")
    fun update(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @RequestBody @Valid request: UpdateCourseRequest
    ): CourseDto {
        val currentUser = currentUser(authentication)
        val name = request.name ?: throw IllegalArgumentException("Course name cannot be empty.")
        val description = request.description ?: throw IllegalArgumentException("Course description cannot be empty.")

        return courseFacade.updateCourse(
            id = courseId,
            name = name,
            description = description,
            actorUserId = currentUser.id
        )
    }

    private fun currentUser(authentication: Authentication): UserDto {
        return userFacade.findUserByEmailOrThrow(authentication.name)
    }
}
