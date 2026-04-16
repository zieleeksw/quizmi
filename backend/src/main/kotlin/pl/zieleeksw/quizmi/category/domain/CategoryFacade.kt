package pl.zieleeksw.quizmi.category.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.category.CategoryDto
import pl.zieleeksw.quizmi.category.CategoryVersionDto
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import java.time.Instant

@Service
class CategoryFacade(
    private val categoryRepository: CategoryRepository,
    private val categoryVersionRepository: CategoryVersionRepository,
    private val courseFacade: CourseFacade,
    private val categoryNameValidator: CategoryNameValidator
) {

    companion object {
        private const val PREVIEW_LIMIT = 9
    }

    @Transactional(readOnly = true)
    fun fetchCategoriesForCourse(
        courseId: Long,
        actorUserId: Long
    ): List<CategoryDto> {
        assertCourseVisibility(courseId)
        val canAccessCourse = courseFacade.hasCourseAccess(courseId, actorUserId)

        return categoryRepository.findAllByCourseIdOrderByNameAsc(courseId)
            .map { it.toDto() }
            .let { categories ->
                if (canAccessCourse) {
                    categories
                } else {
                    categories.take(PREVIEW_LIMIT)
                }
            }
    }

    @Transactional(readOnly = true)
    fun fetchCategoryForCourse(
        courseId: Long,
        categoryId: Long,
        actorUserId: Long
    ): CategoryDto {
        assertCourseOwnership(courseId, actorUserId)
        return findCategoryInCourseOrThrow(categoryId, courseId).toDto()
    }

    @Transactional(readOnly = true)
    fun fetchCategoryVersionsForCourse(
        courseId: Long,
        categoryId: Long,
        actorUserId: Long
    ): List<CategoryVersionDto> {
        assertCourseOwnership(courseId, actorUserId)
        findCategoryInCourseOrThrow(categoryId, courseId)

        return categoryVersionRepository.findAllByCategoryIdOrderByVersionNumberDesc(categoryId)
            .map { it.toDto() }
    }

    @Transactional
    fun createCategory(
        courseId: Long,
        name: String,
        actorUserId: Long
    ): CategoryDto {
        assertCourseOwnership(courseId, actorUserId)

        val normalizedName = name.trim()
        categoryNameValidator.validate(normalizedName)

        if (categoryRepository.existsByCourseIdAndNameIgnoreCase(courseId, normalizedName)) {
            throw IllegalArgumentException("Category name already exists in this course.")
        }

        val now = Instant.now()
        val savedCategory = categoryRepository.save(
            CategoryEntity(
                courseId = courseId,
                name = normalizedName,
                createdAt = now,
                updatedAt = now
            )
        )

        categoryVersionRepository.save(
            CategoryVersionEntity(
                categoryId = savedCategory.id!!,
                versionNumber = 1,
                name = normalizedName,
                createdAt = now
            )
        )

        return savedCategory.toDto()
    }

    @Transactional
    fun updateCategory(
        courseId: Long,
        categoryId: Long,
        name: String,
        actorUserId: Long
    ): CategoryDto {
        assertCourseOwnership(courseId, actorUserId)

        val category = findCategoryInCourseOrThrow(categoryId, courseId)
        val normalizedName = name.trim()
        categoryNameValidator.validate(normalizedName)

        if (category.name.equals(normalizedName, ignoreCase = true)) {
            throw IllegalArgumentException("Category update must change the name.")
        }

        if (categoryRepository.existsByCourseIdAndNameIgnoreCase(courseId, normalizedName)) {
            throw IllegalArgumentException("Category name already exists in this course.")
        }

        val now = Instant.now()
        category.name = normalizedName
        category.updatedAt = now

        val savedCategory = categoryRepository.save(category)
        val nextVersionNumber = (categoryVersionRepository.findTopByCategoryIdOrderByVersionNumberDesc(categoryId)?.versionNumber ?: 0) + 1

        categoryVersionRepository.save(
            CategoryVersionEntity(
                categoryId = categoryId,
                versionNumber = nextVersionNumber,
                name = normalizedName,
                createdAt = now
            )
        )

        return savedCategory.toDto()
    }

    private fun assertCourseOwnership(
        courseId: Long,
        actorUserId: Long
    ) {
        courseFacade.fetchCourseForManager(
            id = courseId,
            actorUserId = actorUserId
        )
    }

    private fun assertCourseVisibility(courseId: Long) {
        courseFacade.assertCourseExists(courseId)
    }

    private fun findCategoryInCourseOrThrow(
        categoryId: Long,
        courseId: Long
    ): CategoryEntity {
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { CategoryNotFoundException.forId(categoryId) }

        if (category.courseId != courseId) {
            throw CategoryNotFoundException.forId(categoryId)
        }

        return category
    }

    private fun CategoryEntity.toDto(): CategoryDto {
        return CategoryDto(
            id = id!!,
            courseId = courseId!!,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun CategoryVersionEntity.toDto(): CategoryVersionDto {
        return CategoryVersionDto(
            id = id!!,
            categoryId = categoryId!!,
            versionNumber = versionNumber!!,
            name = name,
            createdAt = createdAt
        )
    }
}
