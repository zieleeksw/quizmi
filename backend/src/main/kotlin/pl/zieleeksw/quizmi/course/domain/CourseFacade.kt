package pl.zieleeksw.quizmi.course.domain

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.course.CourseDto
import java.time.Instant

@Service
class CourseFacade(
    private val courseRepository: CourseRepository,
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

        return savedCourse.toDto()
    }

    @Transactional(readOnly = true)
    fun fetchCoursesForOwner(ownerUserId: Long): List<CourseDto> {
        return courseRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)
            .map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun fetchCourseForOwner(
        id: Long,
        actorUserId: Long
    ): CourseDto {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }

        assertCanManageCourse(entity, actorUserId)
        return entity.toDto()
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

        return courseRepository.save(entity).toDto()
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

    private fun CourseEntity.toDto(): CourseDto {
        return CourseDto(
            id = id!!,
            name = name,
            description = description,
            createdAt = createdAt,
            ownerUserId = ownerUserId!!
        )
    }
}
