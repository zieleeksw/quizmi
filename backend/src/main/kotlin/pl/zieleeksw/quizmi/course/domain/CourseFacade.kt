package pl.zieleeksw.quizmi.course.domain

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.course.CourseDto
import pl.zieleeksw.quizmi.user.domain.UserEntity
import pl.zieleeksw.quizmi.user.domain.UserRepository
import java.time.Instant

@Service
class CourseFacade(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val courseNameValidator: CourseNameValidator,
    private val courseDescriptionValidator: CourseDescriptionValidator
) {

    @Transactional
    fun createCourse(
        name: String,
        description: String,
        ownerUserId: Long
    ): CourseDto {
        val normalizedName = name.trim()
        val normalizedDescription = description.trim()

        courseNameValidator.validate(normalizedName)
        courseDescriptionValidator.validate(normalizedDescription)

        val savedCourse = courseRepository.save(
            CourseEntity(
                name = normalizedName,
                description = normalizedDescription,
                createdAt = Instant.now(),
                ownerUserId = ownerUserId
            )
        )

        return savedCourse.toDto(
            ownerEmail = readOwnerEmail(savedCourse.ownerUserId!!)
        )
    }

    @Transactional(readOnly = true)
    fun fetchVisibleCourses(): List<CourseDto> {
        val courses = courseRepository.findAllByOrderByCreatedAtDesc()
        val ownersById = userRepository.findAllById(courses.mapNotNull { it.ownerUserId }.distinct())
            .associateBy { it.id!! }

        return courses.map { course ->
            val ownerEmail = ownersById[course.ownerUserId!!]?.email
                ?: throw IllegalStateException("Owner with id ${course.ownerUserId} was not found for course ${course.id}.")

            course.toDto(ownerEmail)
        }
    }

    @Transactional(readOnly = true)
    fun fetchCourseById(id: Long): CourseDto {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }

        return entity.toDto(
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    @Transactional(readOnly = true)
    fun fetchCourseForOwner(
        id: Long,
        actorUserId: Long
    ): CourseDto {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }

        assertCanManageCourse(entity, actorUserId)

        return entity.toDto(
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    @Transactional
    fun updateCourse(
        id: Long,
        name: String,
        description: String,
        actorUserId: Long
    ): CourseDto {
        val normalizedName = name.trim()
        val normalizedDescription = description.trim()

        courseNameValidator.validate(normalizedName)
        courseDescriptionValidator.validate(normalizedDescription)

        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }

        assertCanManageCourse(entity, actorUserId)

        entity.name = normalizedName
        entity.description = normalizedDescription

        return courseRepository.save(entity).toDto(
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    private fun assertCanManageCourse(
        entity: CourseEntity,
        actorUserId: Long
    ) {
        if (entity.ownerUserId == actorUserId) {
            return
        }

        throw AccessDeniedException("You cannot manage this course.")
    }

    private fun readOwnerEmail(ownerUserId: Long): String {
        return userRepository.findById(ownerUserId)
            .map(UserEntity::email)
            .orElseThrow { IllegalStateException("Owner with id $ownerUserId was not found.") }
    }

    private fun CourseEntity.toDto(ownerEmail: String): CourseDto {
        return CourseDto(
            id = id!!,
            name = name,
            description = description,
            createdAt = createdAt,
            ownerUserId = ownerUserId!!,
            ownerEmail = ownerEmail
        )
    }
}
