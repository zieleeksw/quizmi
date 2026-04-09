package pl.zieleeksw.quizmi.category

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.zieleeksw.quizmi.category.domain.CategoryFacade
import pl.zieleeksw.quizmi.user.UserDto
import pl.zieleeksw.quizmi.user.domain.UserFacade

@RestController
@RequestMapping("/courses/{courseId}/categories")
class CategoryController(
    private val categoryFacade: CategoryFacade,
    private val userFacade: UserFacade
) {

    @GetMapping
    fun fetchAll(
        authentication: Authentication,
        @PathVariable courseId: Long
    ): List<CategoryDto> {
        val currentUser = currentUser(authentication)
        return categoryFacade.fetchCategoriesForCourse(
            courseId = courseId,
            actorUserId = currentUser.id
        )
    }

    @GetMapping("/{categoryId}")
    fun fetchById(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable categoryId: Long
    ): CategoryDto {
        val currentUser = currentUser(authentication)
        return categoryFacade.fetchCategoryForCourse(
            courseId = courseId,
            categoryId = categoryId,
            actorUserId = currentUser.id
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @RequestBody @Valid request: CreateCategoryRequest
    ): CategoryDto {
        val currentUser = currentUser(authentication)
        val name = request.name ?: throw IllegalArgumentException("Category name cannot be empty.")

        return categoryFacade.createCategory(
            courseId = courseId,
            name = name,
            actorUserId = currentUser.id
        )
    }

    @PutMapping("/{categoryId}")
    fun update(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable categoryId: Long,
        @RequestBody @Valid request: UpdateCategoryRequest
    ): CategoryDto {
        val currentUser = currentUser(authentication)
        val name = request.name ?: throw IllegalArgumentException("Category name cannot be empty.")

        return categoryFacade.updateCategory(
            courseId = courseId,
            categoryId = categoryId,
            name = name,
            actorUserId = currentUser.id
        )
    }

    @GetMapping("/{categoryId}/versions")
    fun fetchVersions(
        authentication: Authentication,
        @PathVariable courseId: Long,
        @PathVariable categoryId: Long
    ): List<CategoryVersionDto> {
        val currentUser = currentUser(authentication)
        return categoryFacade.fetchCategoryVersionsForCourse(
            courseId = courseId,
            categoryId = categoryId,
            actorUserId = currentUser.id
        )
    }

    private fun currentUser(authentication: Authentication): UserDto {
        return userFacade.findUserByEmailOrThrow(authentication.name)
    }
}
