package pl.zieleeksw.quizmi.quiz.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.category.domain.CategoryRepository
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import pl.zieleeksw.quizmi.question.domain.QuestionFacade
import pl.zieleeksw.quizmi.question.domain.QuestionSummary
import pl.zieleeksw.quizmi.quiz.QuizCategoryDto
import pl.zieleeksw.quizmi.quiz.QuizDto
import pl.zieleeksw.quizmi.quiz.QuizMode
import pl.zieleeksw.quizmi.quiz.QuizOrderMode
import pl.zieleeksw.quizmi.quiz.QuizVersionDto
import java.time.Instant
import kotlin.math.min

@Service
class QuizFacade(
    private val quizRepository: QuizRepository,
    private val quizVersionRepository: QuizVersionRepository,
    private val quizVersionQuestionRepository: QuizVersionQuestionRepository,
    private val quizVersionCategoryRepository: QuizVersionCategoryRepository,
    private val courseFacade: CourseFacade,
    private val categoryRepository: CategoryRepository,
    private val questionFacade: QuestionFacade
) {

    @Transactional
    fun createQuiz(
        courseId: Long,
        title: String,
        mode: QuizMode,
        randomCount: Int?,
        questionOrder: QuizOrderMode,
        answerOrder: QuizOrderMode,
        questionIds: List<Long>,
        categoryIds: List<Long>,
        actorUserId: Long
    ): QuizDto {
        assertCourseOwnership(courseId, actorUserId)
        val questionSummaries = questionFacade.fetchQuestionSummaries(courseId, actorUserId)
        val draft = validateAndCreateDraft(courseId, questionSummaries, title, mode, randomCount, questionOrder, answerOrder, questionIds, categoryIds)
        val now = roundToDatabasePrecision(Instant.now())
        val savedQuiz = quizRepository.save(
            QuizEntity(
                courseId = courseId,
                active = true,
                currentVersionNumber = 1,
                createdAt = now,
                updatedAt = now
            )
        )

        saveVersion(savedQuiz.id!!, 1, draft, now)
        return toCurrentQuizDto(savedQuiz, questionSummaries)
    }

    @Transactional(readOnly = true)
    fun fetchQuizzes(
        courseId: Long,
        actorUserId: Long
    ): List<QuizDto> {
        assertCourseVisibility(courseId)
        val canAccessCourse = courseFacade.hasCourseAccess(courseId, actorUserId)
        val quizzes = if (canAccessCourse) {
            quizRepository.findAllByCourseIdAndActiveTrueOrderByCreatedAtDesc(courseId)
        } else {
            quizRepository.findTop3ByCourseIdAndActiveTrueOrderByCreatedAtDesc(courseId)
        }

        if (quizzes.isEmpty()) {
            return emptyList()
        }

        val questionSummaries = questionFacade.fetchQuestionSummaries(courseId, actorUserId)
        return toCurrentQuizDtos(quizzes, questionSummaries)
    }

    @Transactional(readOnly = true)
    fun fetchQuizForCourse(
        courseId: Long,
        quizId: Long,
        actorUserId: Long
    ): QuizDto {
        courseFacade.fetchCourseForMember(courseId, actorUserId)
        val questionSummaries = questionFacade.fetchQuestionSummaries(courseId, actorUserId)
        return toCurrentQuizDto(findActiveQuizInCourseOrThrow(quizId, courseId), questionSummaries)
    }

    @Transactional(readOnly = true)
    fun assertActiveQuizVisible(
        courseId: Long,
        quizId: Long,
        actorUserId: Long
    ) {
        courseFacade.fetchCourseForMember(courseId, actorUserId)
        findActiveQuizInCourseOrThrow(quizId, courseId)
    }

    @Transactional(readOnly = true)
    fun fetchQuizVersions(
        courseId: Long,
        quizId: Long,
        actorUserId: Long
    ): List<QuizVersionDto> {
        assertCourseOwnership(courseId, actorUserId)
        val questionSummaries = questionFacade.fetchQuestionSummaries(courseId, actorUserId)
        val quiz = findActiveQuizInCourseOrThrow(quizId, courseId)

        return quizVersionRepository.findAllByQuizIdOrderByVersionNumberDesc(quiz.id!!)
            .map { toQuizVersionDto(courseId, it, questionSummaries) }
    }

    @Transactional
    fun updateQuiz(
        courseId: Long,
        quizId: Long,
        title: String,
        mode: QuizMode,
        randomCount: Int?,
        questionOrder: QuizOrderMode,
        answerOrder: QuizOrderMode,
        questionIds: List<Long>,
        categoryIds: List<Long>,
        actorUserId: Long
    ): QuizDto {
        assertCourseOwnership(courseId, actorUserId)
        val questionSummaries = questionFacade.fetchQuestionSummaries(courseId, actorUserId)
        val quiz = findActiveQuizInCourseOrThrow(quizId, courseId)
        val draft = validateAndCreateDraft(courseId, questionSummaries, title, mode, randomCount, questionOrder, answerOrder, questionIds, categoryIds)
        val currentVersion = quizVersionRepository.findByQuizIdAndVersionNumber(quiz.id!!, quiz.currentVersionNumber!!)
            .orElseThrow { IllegalStateException("Current quiz version was not found.") }

        assertMeaningfulUpdate(currentVersion, draft)

        val now = roundToDatabasePrecision(Instant.now())
        quiz.currentVersionNumber = quiz.currentVersionNumber!! + 1
        quiz.updatedAt = now
        val savedQuiz = quizRepository.save(quiz)

        saveVersion(savedQuiz.id!!, savedQuiz.currentVersionNumber!!, draft, now)
        return toCurrentQuizDto(savedQuiz, questionSummaries)
    }

    @Transactional
    fun deleteQuiz(
        courseId: Long,
        quizId: Long,
        actorUserId: Long
    ) {
        assertCourseOwnership(courseId, actorUserId)
        val quiz = findActiveQuizInCourseOrThrow(quizId, courseId)
        quiz.active = false
        quiz.updatedAt = roundToDatabasePrecision(Instant.now())
        quizRepository.save(quiz)
    }

    private fun validateAndCreateDraft(
        courseId: Long,
        questionSummaries: List<QuestionSummary>,
        title: String,
        mode: QuizMode,
        randomCount: Int?,
        questionOrder: QuizOrderMode,
        answerOrder: QuizOrderMode,
        questionIds: List<Long>,
        categoryIds: List<Long>
    ): QuizDraft {
        val normalizedTitle = title.trim()
        val normalizedQuestionIds = normalizeIds(questionIds)
        val normalizedCategoryIds = normalizeIds(categoryIds)

        validateTitle(normalizedTitle)

        when (mode) {
            QuizMode.MANUAL -> {
                if (normalizedQuestionIds.isEmpty()) {
                    throw IllegalArgumentException("Manual quiz must contain at least 1 question.")
                }
                if (randomCount != null) {
                    throw IllegalArgumentException("Manual quiz cannot define a random question count.")
                }
                if (normalizedCategoryIds.isNotEmpty()) {
                    throw IllegalArgumentException("Manual quiz cannot define category filters.")
                }

                assertQuestionsBelongToCourse(questionSummaries, normalizedQuestionIds)
            }

            QuizMode.RANDOM -> {
                if (randomCount == null || randomCount < 1) {
                    throw IllegalArgumentException("Random quiz must define a random count greater than 0.")
                }
                if (normalizedQuestionIds.isNotEmpty()) {
                    throw IllegalArgumentException("Random quiz cannot contain manually selected questions.")
                }
                if (normalizedCategoryIds.isNotEmpty()) {
                    throw IllegalArgumentException("Random quiz cannot define category filters.")
                }
                if (questionSummaries.isEmpty()) {
                    throw IllegalArgumentException("Random quiz requires at least 1 course question.")
                }
            }

            QuizMode.CATEGORY -> {
                if (normalizedCategoryIds.isEmpty()) {
                    throw IllegalArgumentException("Category quiz must define at least 1 category.")
                }
                if (randomCount == null || randomCount < 1) {
                    throw IllegalArgumentException("Category quiz must define a question count greater than 0.")
                }
                if (normalizedQuestionIds.isNotEmpty()) {
                    throw IllegalArgumentException("Category quiz cannot contain manually selected questions.")
                }

                val categoriesById = categoryRepository.findAllByCourseIdAndIdIn(courseId, normalizedCategoryIds).associateBy { it.id!! }

                if (categoriesById.size != normalizedCategoryIds.size) {
                    throw IllegalArgumentException("Some selected categories are invalid for this course.")
                }

                val matchedAnyQuestion = questionSummaries.any { question ->
                    question.categoryIds.any { categoryId -> normalizedCategoryIds.contains(categoryId) }
                }

                if (!matchedAnyQuestion) {
                    throw IllegalArgumentException("Selected categories do not match any current course question.")
                }
            }
        }

        return QuizDraft(
            title = normalizedTitle,
            mode = mode,
            randomCount = if (mode == QuizMode.RANDOM || mode == QuizMode.CATEGORY) randomCount else null,
            questionOrder = questionOrder,
            answerOrder = answerOrder,
            questionIds = if (mode == QuizMode.MANUAL) normalizedQuestionIds else emptyList(),
            categoryIds = if (mode == QuizMode.CATEGORY) normalizedCategoryIds else emptyList()
        )
    }

    private fun validateTitle(title: String) {
        if (title.isBlank()) {
            throw IllegalArgumentException("Quiz title is required.")
        }

        if (title.length !in 4..120) {
            throw IllegalArgumentException("Quiz title must contain between 4 and 120 characters.")
        }
    }

    private fun assertQuestionsBelongToCourse(
        questionSummaries: List<QuestionSummary>,
        questionIds: List<Long>
    ) {
        val availableQuestionIds = questionSummaries.map { it.id }.toSet()

        if (!availableQuestionIds.containsAll(questionIds)) {
            throw IllegalArgumentException("Some selected questions are invalid for this course.")
        }
    }

    private fun saveVersion(
        quizId: Long,
        versionNumber: Int,
        draft: QuizDraft,
        createdAt: Instant
    ) {
        val savedVersion = quizVersionRepository.save(
            QuizVersionEntity(
                quizId = quizId,
                versionNumber = versionNumber,
                title = draft.title,
                mode = draft.mode,
                randomCount = draft.randomCount,
                questionOrder = draft.questionOrder,
                answerOrder = draft.answerOrder,
                createdAt = createdAt
            )
        )

        if (draft.questionIds.isNotEmpty()) {
            quizVersionQuestionRepository.saveAll(
                draft.questionIds.mapIndexed { index, questionId ->
                    QuizVersionQuestionEntity(savedVersion.id!!, questionId, index)
                }
            )
        }

        if (draft.categoryIds.isNotEmpty()) {
            quizVersionCategoryRepository.saveAll(
                draft.categoryIds.mapIndexed { index, categoryId ->
                    QuizVersionCategoryEntity(savedVersion.id!!, categoryId, index)
                }
            )
        }
    }

    private fun findQuizInCourseOrThrow(quizId: Long, courseId: Long): QuizEntity {
        val quiz = quizRepository.findById(quizId)
            .orElseThrow { QuizNotFoundException.forId(quizId) }

        if (quiz.courseId != courseId) {
            throw QuizNotFoundException.forId(quizId)
        }

        return quiz
    }

    private fun findActiveQuizInCourseOrThrow(quizId: Long, courseId: Long): QuizEntity {
        val quiz = findQuizInCourseOrThrow(quizId, courseId)

        if (!quiz.active) {
            throw QuizNotFoundException.forId(quizId)
        }

        return quiz
    }

    private fun toCurrentQuizDto(
        quiz: QuizEntity,
        questionSummaries: List<QuestionSummary>
    ): QuizDto {
        return toCurrentQuizDtos(listOf(quiz), questionSummaries).single()
    }

    private fun toCurrentQuizDtos(
        quizzes: List<QuizEntity>,
        questionSummaries: List<QuestionSummary>
    ): List<QuizDto> {
        if (quizzes.isEmpty()) {
            return emptyList()
        }

        val courseId = quizzes.first().courseId ?: throw IllegalStateException("Quiz course id is missing.")
        val currentVersionsByQuizId = quizVersionRepository.findCurrentVersionsByQuizIds(quizzes.mapNotNull { it.id })
            .associateBy { it.quizId!! }
        val currentVersions = quizzes.map { quiz ->
            val quizId = quiz.id ?: throw IllegalStateException("Quiz id is missing.")
            currentVersionsByQuizId[quizId]
                ?: throw IllegalStateException("Current quiz version was not found.")
        }
        val versionIds = currentVersions.mapNotNull { it.id }
        val questionIdsByQuizVersionId = quizVersionQuestionRepository.findAllByQuizVersionIdInOrderByQuizVersionIdAscDisplayOrderAsc(versionIds)
            .groupBy { it.quizVersionId!! }
            .mapValues { (_, entities) -> entities.mapNotNull { it.questionId } }
        val categoryIdsByQuizVersionId = quizVersionCategoryRepository.findAllByQuizVersionIdInOrderByQuizVersionIdAscDisplayOrderAsc(versionIds)
            .groupBy { it.quizVersionId!! }
            .mapValues { (_, entities) -> entities.mapNotNull { it.categoryId } }
        val allCategoryIds = categoryIdsByQuizVersionId.values.flatten().distinct()
        val categoriesById = if (allCategoryIds.isEmpty()) {
            emptyMap()
        } else {
            categoryRepository.findAllByCourseIdAndIdIn(courseId, allCategoryIds).associateBy { it.id!! }
        }
        val availableQuestionIds = questionSummaries.map { it.id }.toSet()
        val availableCategoryIdsByQuestionId = questionSummaries.associate { it.id to it.categoryIds }
        val availableQuestionCount = questionSummaries.size

        return quizzes.map { quiz ->
            val quizId = quiz.id ?: throw IllegalStateException("Quiz id is missing.")
            val currentVersion = currentVersionsByQuizId[quizId]
                ?: throw IllegalStateException("Current quiz version was not found.")
            val currentVersionId = currentVersion.id ?: throw IllegalStateException("Quiz version id is missing.")
            val questionIds = questionIdsByQuizVersionId[currentVersionId].orEmpty()
            val categoryIds = categoryIdsByQuizVersionId[currentVersionId].orEmpty()

            QuizDto(
                id = quizId,
                courseId = quiz.courseId!!,
                active = quiz.active,
                currentVersionNumber = quiz.currentVersionNumber!!,
                createdAt = quiz.createdAt,
                updatedAt = quiz.updatedAt,
                title = currentVersion.title,
                mode = currentVersion.mode!!,
                randomCount = currentVersion.randomCount,
                questionOrder = currentVersion.questionOrder!!,
                answerOrder = currentVersion.answerOrder!!,
                questionIds = questionIds,
                categories = categoryIds.mapNotNull { categoryId ->
                    categoriesById[categoryId]?.let { QuizCategoryDto(it.id!!, it.name) }
                },
                resolvedQuestionCount = resolveQuestionCount(
                    version = currentVersion,
                    questionIds = questionIds,
                    categoryIds = categoryIds.toSet(),
                    availableQuestionIds = availableQuestionIds,
                    availableCategoryIdsByQuestionId = availableCategoryIdsByQuestionId,
                    availableQuestionCount = availableQuestionCount
                )
            )
        }
    }

    private fun toQuizVersionDto(
        courseId: Long,
        version: QuizVersionEntity,
        questionSummaries: List<QuestionSummary>
    ): QuizVersionDto {
        val questionIds = quizVersionQuestionRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(version.id!!).map { it.questionId!! }
        val categoryIds = quizVersionCategoryRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(version.id!!)
            .map { it.categoryId!! }

        return QuizVersionDto(
            id = version.id!!,
            quizId = version.quizId!!,
            versionNumber = version.versionNumber!!,
            createdAt = version.createdAt,
            title = version.title,
            mode = version.mode!!,
            randomCount = version.randomCount,
            questionOrder = version.questionOrder!!,
            answerOrder = version.answerOrder!!,
            questionIds = questionIds,
            categories = findCategoryDtos(courseId, categoryIds),
            resolvedQuestionCount = resolveQuestionCount(
                version = version,
                questionIds = questionIds,
                categoryIds = categoryIds.toSet(),
                availableQuestionIds = questionSummaries.map { it.id }.toSet(),
                availableCategoryIdsByQuestionId = questionSummaries.associate { it.id to it.categoryIds },
                availableQuestionCount = questionSummaries.size
            )
        )
    }

    private fun findCategoryDtos(courseId: Long, categoryIds: List<Long>): List<QuizCategoryDto> {
        if (categoryIds.isEmpty()) {
            return emptyList()
        }

        val categoriesById = categoryRepository.findAllByCourseIdAndIdIn(courseId, categoryIds).associateBy { it.id!! }

        return categoryIds.mapNotNull { categoryId ->
            categoriesById[categoryId]?.let { QuizCategoryDto(it.id!!, it.name) }
        }
    }

    private fun resolveQuestionCount(
        version: QuizVersionEntity,
        questionIds: List<Long>,
        categoryIds: Set<Long>,
        availableQuestionIds: Set<Long>,
        availableCategoryIdsByQuestionId: Map<Long, Set<Long>>,
        availableQuestionCount: Int
    ): Int {
        return when (version.mode!!) {
            QuizMode.MANUAL -> questionIds.count { availableQuestionIds.contains(it) }

            QuizMode.RANDOM -> min(version.randomCount ?: availableQuestionCount, availableQuestionCount)

            QuizMode.CATEGORY -> {
                val matchingQuestions = availableCategoryIdsByQuestionId.values
                    .count { questionCategoryIds -> questionCategoryIds.any { categoryIds.contains(it) } }
                min(version.randomCount ?: matchingQuestions, matchingQuestions)
            }
        }
    }

    private fun assertMeaningfulUpdate(currentVersion: QuizVersionEntity, draft: QuizDraft) {
        val currentQuestionIds = quizVersionQuestionRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(currentVersion.id!!)
            .map { it.questionId!! }
        val currentCategoryIds = quizVersionCategoryRepository.findAllByQuizVersionIdOrderByDisplayOrderAsc(currentVersion.id!!)
            .map { it.categoryId!! }

        if (
            currentVersion.title == draft.title &&
            currentVersion.mode == draft.mode &&
            currentVersion.randomCount == draft.randomCount &&
            currentVersion.questionOrder == draft.questionOrder &&
            currentVersion.answerOrder == draft.answerOrder &&
            currentQuestionIds == draft.questionIds &&
            currentCategoryIds == draft.categoryIds
        ) {
            throw IllegalArgumentException(
                "Quiz update must change the title, mode, random settings, order settings, questions, or categories."
            )
        }
    }

    private fun normalizeIds(ids: List<Long>): List<Long> {
        return ids.fold(linkedSetOf<Long>()) { acc, id ->
            acc.apply { add(id) }
        }.toList()
    }

    private fun roundToDatabasePrecision(instant: Instant): Instant {
        var epochSecond = instant.epochSecond
        var roundedMicros = (instant.nano + 500L) / 1_000L

        if (roundedMicros == 1_000_000L) {
            epochSecond++
            roundedMicros = 0L
        }

        return Instant.ofEpochSecond(epochSecond, roundedMicros * 1_000L)
    }

    private fun assertCourseOwnership(courseId: Long, actorUserId: Long) {
        courseFacade.fetchCourseForManager(courseId, actorUserId)
    }

    private fun assertCourseVisibility(courseId: Long) {
        courseFacade.assertCourseExists(courseId)
    }

    private data class QuizDraft(
        val title: String,
        val mode: QuizMode,
        val randomCount: Int?,
        val questionOrder: QuizOrderMode,
        val answerOrder: QuizOrderMode,
        val questionIds: List<Long>,
        val categoryIds: List<Long>
    )
}
