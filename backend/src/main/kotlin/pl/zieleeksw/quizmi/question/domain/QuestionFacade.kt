package pl.zieleeksw.quizmi.question.domain

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.category.domain.CategoryEntity
import pl.zieleeksw.quizmi.category.domain.CategoryRepository
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import pl.zieleeksw.quizmi.question.QuestionAnswerDto
import pl.zieleeksw.quizmi.question.QuestionAnswerRequest
import pl.zieleeksw.quizmi.question.QuestionCategoryDto
import pl.zieleeksw.quizmi.question.QuestionDto
import pl.zieleeksw.quizmi.question.QuestionPageDto
import pl.zieleeksw.quizmi.question.QuestionVersionDto
import java.time.Instant

@Service
class QuestionFacade(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val questionAnswerRepository: QuestionAnswerRepository,
    private val questionVersionCategoryRepository: QuestionVersionCategoryRepository,
    private val categoryRepository: CategoryRepository,
    private val courseFacade: CourseFacade,
    private val questionPromptValidator: QuestionPromptValidator,
    private val questionExplanationValidator: QuestionExplanationValidator,
    private val questionAnswersValidator: QuestionAnswersValidator,
    private val questionCategoryIdsValidator: QuestionCategoryIdsValidator
) {

    companion object {
        private const val PREVIEW_LIMIT = 5
    }

    @Transactional
    fun createQuestion(
        courseId: Long,
        prompt: String,
        explanation: String?,
        answers: List<QuestionAnswerRequest>,
        categoryIds: List<Long>,
        actorUserId: Long
    ): QuestionDto {
        assertCourseOwnership(courseId, actorUserId)

        val normalizedPrompt = prompt.trim()
        val normalizedExplanation = normalizeExplanation(explanation)
        val normalizedAnswers = normalizeAnswers(answers)
        val normalizedCategoryIds = categoryIds.toList()

        questionPromptValidator.validate(normalizedPrompt)
        questionExplanationValidator.validate(normalizedExplanation)
        questionAnswersValidator.validate(answers)
        questionCategoryIdsValidator.validate(normalizedCategoryIds)
        val categories = findCategoriesInCourseOrThrow(courseId, normalizedCategoryIds)

        val now = Instant.now()
        val savedQuestion = questionRepository.save(
            QuestionEntity(
                courseId = courseId,
                currentVersionNumber = 1,
                createdAt = now,
                updatedAt = now
            )
        )

        saveVersion(
            questionId = savedQuestion.id!!,
            versionNumber = 1,
            prompt = normalizedPrompt,
            explanation = normalizedExplanation,
            createdAt = now,
            answers = normalizedAnswers,
            categories = categories
        )

        return toCurrentQuestionDto(savedQuestion)
    }

    @Transactional(readOnly = true)
    fun fetchQuestionListing(
        courseId: Long,
        actorUserId: Long
    ): List<QuestionDto> {
        assertCourseVisibility(courseId)
        val canAccessCourse = courseFacade.hasCourseAccess(courseId, actorUserId)

        return loadCurrentQuestions(courseId)
            .let { questions ->
                if (canAccessCourse) {
                    questions
                } else {
                    questions.take(PREVIEW_LIMIT)
                }
            }
    }

    @Transactional(readOnly = true)
    fun fetchQuestions(
        courseId: Long,
        actorUserId: Long
    ): List<QuestionDto> {
        assertCourseVisibility(courseId)
        return loadCurrentQuestions(courseId)
    }

    @Transactional(readOnly = true)
    fun fetchQuestionsByIds(
        courseId: Long,
        actorUserId: Long,
        questionIds: List<Long>
    ): List<QuestionDto> {
        assertCourseVisibility(courseId)

        if (questionIds.isEmpty()) {
            return emptyList()
        }

        val questionsById = questionRepository.findAllByCourseIdAndIdIn(courseId, questionIds)
            .associateBy { it.id!! }

        return questionIds.mapNotNull { questionId ->
            questionsById[questionId]?.let(::toCurrentQuestionDto)
        }
    }

    @Transactional(readOnly = true)
    fun fetchQuestionPreview(
        courseId: Long,
        actorUserId: Long,
        page: Int,
        size: Int,
        search: String?,
        categoryId: Long?
    ): QuestionPageDto {
        assertCourseVisibility(courseId)
        val canAccessCourse = courseFacade.hasCourseAccess(courseId, actorUserId)

        if (!canAccessCourse) {
            assertPreviewRequestAllowedForLockedCourse(page, size, search, categoryId)
        }

        val normalizedPage = page.coerceAtLeast(0)
        val normalizedSize = size.coerceIn(1, 50)
        val normalizedSearch = search?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

        val filteredQuestions = loadCurrentQuestions(courseId)
            .let { questions ->
                if (canAccessCourse) {
                    questions
                } else {
                    questions.take(PREVIEW_LIMIT)
                }
            }
            .filter { matchesCategory(it, categoryId) }
            .filter { matchesSearch(it, normalizedSearch) }

        val totalItems = filteredQuestions.size.toLong()
        val totalPages = if (totalItems == 0L) 0 else kotlin.math.ceil(totalItems.toDouble() / normalizedSize.toDouble()).toInt()
        val safePage = if (totalPages == 0) 0 else normalizedPage.coerceAtMost(totalPages - 1)
        val fromIndex = safePage * normalizedSize
        val toIndex = minOf(fromIndex + normalizedSize, filteredQuestions.size)
        val items = if (totalItems == 0L) emptyList() else filteredQuestions.subList(fromIndex, toIndex)

        return QuestionPageDto(
            items = items,
            pageNumber = safePage,
            pageSize = normalizedSize,
            totalItems = totalItems,
            totalPages = totalPages,
            hasNext = totalPages > 0 && safePage < totalPages - 1,
            hasPrevious = safePage > 0
        )
    }

    @Transactional(readOnly = true)
    fun fetchQuestionForCourse(
        courseId: Long,
        questionId: Long,
        actorUserId: Long
    ): QuestionDto {
        assertCourseOwnership(courseId, actorUserId)
        return toCurrentQuestionDto(findQuestionInCourseOrThrow(questionId, courseId))
    }

    @Transactional(readOnly = true)
    fun fetchQuestionVersions(
        courseId: Long,
        questionId: Long,
        actorUserId: Long
    ): List<QuestionVersionDto> {
        assertCourseOwnership(courseId, actorUserId)
        val question = findQuestionInCourseOrThrow(questionId, courseId)

        return questionVersionRepository.findAllByQuestionIdOrderByVersionNumberDesc(question.id!!)
            .map { toQuestionVersionDto(courseId, it) }
    }

    @Transactional
    fun updateQuestion(
        courseId: Long,
        questionId: Long,
        prompt: String,
        explanation: String?,
        answers: List<QuestionAnswerRequest>,
        categoryIds: List<Long>,
        actorUserId: Long
    ): QuestionDto {
        assertCourseOwnership(courseId, actorUserId)

        val question = findQuestionInCourseOrThrow(questionId, courseId)
        val normalizedPrompt = prompt.trim()
        val normalizedExplanation = normalizeExplanation(explanation)
        val normalizedAnswers = normalizeAnswers(answers)
        val normalizedCategoryIds = categoryIds.toList()

        questionPromptValidator.validate(normalizedPrompt)
        questionExplanationValidator.validate(normalizedExplanation)
        questionAnswersValidator.validate(answers)
        questionCategoryIdsValidator.validate(normalizedCategoryIds)
        val categories = findCategoriesInCourseOrThrow(courseId, normalizedCategoryIds)
        val currentVersion = questionVersionRepository.findByQuestionIdAndVersionNumber(
            question.id!!,
            question.currentVersionNumber!!
        ).orElseThrow { IllegalStateException("Current question version was not found.") }

        assertMeaningfulUpdate(currentVersion, normalizedPrompt, normalizedExplanation, normalizedAnswers, normalizedCategoryIds)

        val now = Instant.now()
        question.currentVersionNumber = question.currentVersionNumber!! + 1
        question.updatedAt = now
        val savedQuestion = questionRepository.save(question)

        saveVersion(
            questionId = savedQuestion.id!!,
            versionNumber = savedQuestion.currentVersionNumber!!,
            prompt = normalizedPrompt,
            explanation = normalizedExplanation,
            createdAt = now,
            answers = normalizedAnswers,
            categories = categories
        )

        return toCurrentQuestionDto(savedQuestion)
    }

    private fun saveVersion(
        questionId: Long,
        versionNumber: Int,
        prompt: String,
        explanation: String?,
        createdAt: Instant,
        answers: List<NormalizedQuestionAnswer>,
        categories: List<CategoryEntity>
    ) {
        val savedVersion = questionVersionRepository.save(
            QuestionVersionEntity(
                questionId = questionId,
                versionNumber = versionNumber,
                prompt = prompt,
                explanation = explanation,
                createdAt = createdAt
            )
        )

        questionAnswerRepository.saveAll(
            answers.map {
                QuestionAnswerEntity(
                    questionVersionId = savedVersion.id!!,
                    displayOrder = it.displayOrder,
                    content = it.content,
                    correct = it.correct
                )
            }
        )

        questionVersionCategoryRepository.saveAll(
            categories.mapIndexed { index, category ->
                QuestionVersionCategoryEntity(
                    questionVersionId = savedVersion.id!!,
                    categoryId = category.id!!,
                    displayOrder = index
                )
            }
        )
    }

    private fun assertMeaningfulUpdate(
        currentVersion: QuestionVersionEntity,
        normalizedPrompt: String,
        normalizedExplanation: String?,
        normalizedAnswers: List<NormalizedQuestionAnswer>,
        categoryIds: List<Long>
    ) {
        val currentAnswers = questionAnswerRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(currentVersion.id!!)
            .map {
                NormalizedQuestionAnswer(
                    displayOrder = it.displayOrder!!,
                    content = it.content,
                    correct = it.correct == true
                )
            }
        val currentCategoryIds = questionVersionCategoryRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(currentVersion.id!!)
            .map { it.categoryId!! }

        if (
            currentVersion.prompt == normalizedPrompt &&
            currentVersion.explanation == normalizedExplanation &&
            currentAnswers == normalizedAnswers &&
            currentCategoryIds == categoryIds
        ) {
            throw IllegalArgumentException("Question update must change the prompt, explanation, answers, or categories.")
        }
    }

    private fun findQuestionInCourseOrThrow(
        questionId: Long,
        courseId: Long
    ): QuestionEntity {
        val question = questionRepository.findById(questionId)
            .orElseThrow { QuestionNotFoundException.forId(questionId) }

        if (question.courseId != courseId) {
            throw QuestionNotFoundException.forId(questionId)
        }

        return question
    }

    private fun findCategoriesInCourseOrThrow(
        courseId: Long,
        categoryIds: List<Long>
    ): List<CategoryEntity> {
        val categoriesById = categoryRepository.findAllByCourseIdAndIdIn(courseId, categoryIds)
            .associateBy { it.id!! }

        if (categoriesById.size != categoryIds.size) {
            throw IllegalArgumentException("Some selected categories are invalid for this course.")
        }

        return categoryIds.map { categoriesById[it]!! }
    }

    private fun loadCurrentQuestions(courseId: Long): List<QuestionDto> {
        return questionRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId)
            .map { toCurrentQuestionDto(it) }
    }

    private fun toCurrentQuestionDto(question: QuestionEntity): QuestionDto {
        val currentVersion = questionVersionRepository.findByQuestionIdAndVersionNumber(
            question.id!!,
            question.currentVersionNumber!!
        ).orElseThrow { IllegalStateException("Current question version was not found.") }

        return QuestionDto(
            id = question.id!!,
            courseId = question.courseId!!,
            currentVersionNumber = question.currentVersionNumber!!,
            createdAt = question.createdAt,
            updatedAt = question.updatedAt,
            prompt = currentVersion.prompt,
            explanation = currentVersion.explanation,
            categories = findCategoryDtos(question.courseId!!, currentVersion.id!!),
            answers = findAnswerDtos(currentVersion.id!!)
        )
    }

    private fun toQuestionVersionDto(
        courseId: Long,
        version: QuestionVersionEntity
    ): QuestionVersionDto {
        return QuestionVersionDto(
            id = version.id!!,
            questionId = version.questionId!!,
            versionNumber = version.versionNumber!!,
            createdAt = version.createdAt,
            prompt = version.prompt,
            explanation = version.explanation,
            categories = findCategoryDtos(courseId, version.id!!),
            answers = findAnswerDtos(version.id!!)
        )
    }

    private fun findCategoryDtos(
        courseId: Long,
        questionVersionId: Long
    ): List<QuestionCategoryDto> {
        val categoryIds = questionVersionCategoryRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId)
            .map { it.categoryId!! }
        val categoriesById = categoryRepository.findAllByCourseIdAndIdIn(courseId, categoryIds)
            .associateBy { it.id!! }

        return categoryIds.mapNotNull { categoryId ->
            categoriesById[categoryId]?.let { QuestionCategoryDto(id = it.id!!, name = it.name) }
        }
    }

    private fun findAnswerDtos(questionVersionId: Long): List<QuestionAnswerDto> {
        return questionAnswerRepository.findAllByQuestionVersionIdOrderByDisplayOrderAsc(questionVersionId)
            .map {
                QuestionAnswerDto(
                    id = it.id!!,
                    displayOrder = it.displayOrder!!,
                    content = it.content,
                    correct = it.correct == true
                )
            }
    }

    private fun normalizeAnswers(answers: List<QuestionAnswerRequest>): List<NormalizedQuestionAnswer> {
        return answers.mapIndexed { index, answer ->
            NormalizedQuestionAnswer(
                displayOrder = index,
                content = answer.content?.trim().orEmpty(),
                correct = answer.correct == true
            )
        }
    }

    private fun normalizeExplanation(explanation: String?): String? {
        return explanation?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun matchesCategory(question: QuestionDto, categoryId: Long?): Boolean {
        if (categoryId == null) {
            return true
        }

        return question.categories.any { it.id == categoryId }
    }

    private fun matchesSearch(question: QuestionDto, normalizedSearch: String?): Boolean {
        if (normalizedSearch.isNullOrBlank()) {
            return true
        }

        return question.prompt.lowercase().contains(normalizedSearch) ||
            question.categories.any { it.name.lowercase().contains(normalizedSearch) }
    }

    private fun assertPreviewRequestAllowedForLockedCourse(
        page: Int,
        size: Int,
        search: String?,
        categoryId: Long?
    ) {
        if (page > 0 || size > PREVIEW_LIMIT || !search.isNullOrBlank() || categoryId != null) {
            throw AccessDeniedException("Locked course previews are limited to the default first page.")
        }
    }

    private fun assertCourseOwnership(
        courseId: Long,
        actorUserId: Long
    ) {
        courseFacade.fetchCourseForManager(id = courseId, actorUserId = actorUserId)
    }

    private fun assertCourseVisibility(courseId: Long) {
        courseFacade.assertCourseExists(courseId)
    }

    private data class NormalizedQuestionAnswer(
        val displayOrder: Int,
        val content: String,
        val correct: Boolean
    )
}
