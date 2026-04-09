package pl.zieleeksw.quizmi.course

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.security.core.Authentication
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
        val currentUser = currentUser(authentication)
        return courseFacade.fetchCoursesForOwner(currentUser.id)
    }

    @GetMapping("/{courseId}")
    fun fetchById(
        authentication: Authentication,
        @PathVariable courseId: Long
    ): CourseDto {
        val currentUser = currentUser(authentication)
        return courseFacade.fetchCourseForOwner(
            id = courseId,
            actorUserId = currentUser.id
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
