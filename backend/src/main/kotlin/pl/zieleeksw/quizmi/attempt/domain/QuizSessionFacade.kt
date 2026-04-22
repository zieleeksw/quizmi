package pl.zieleeksw.quizmi.attempt.domain

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.attempt.QuizAttemptAnswerRequest
import pl.zieleeksw.quizmi.attempt.QuizSessionDto
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import pl.zieleeksw.quizmi.question.domain.QuestionFacade
import pl.zieleeksw.quizmi.question.domain.QuestionSummary
import pl.zieleeksw.quizmi.quiz.QuizMode
import pl.zieleeksw.quizmi.quiz.QuizOrderMode
import pl.zieleeksw.quizmi.quiz.domain.QuizFacade
import pl.zieleeksw.quizmi.quiz.domain.QuizSessionSpec
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

@Service
class QuizSessionFacade(
    private val quizSessionRepository: QuizSessionRepository,
    private val courseFacade: CourseFacade,
    private val quizFacade: QuizFacade,
    private val questionFacade: QuestionFacade,
    private val objectMapper: ObjectMapper
) {

    @Transactional(readOnly = true)
    fun fetchCourseSessions(courseId: Long, userId: Long): List<QuizSessionDto> {
        assertCourseVisibility(courseId, userId)
        return quizSessionRepository.findAllByCourseIdAndUserIdOrderByUpdatedAtDesc(courseId, userId).map { toDto(it) }
    }

    @Transactional
    fun createOrResumeSession(courseId: Long, quizId: Long, userId: Long): QuizSessionDto {
        val quiz = quizFacade.fetchQuizSessionSpec(courseId, quizId, userId)

        return quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
            .map { synchronizeSessionState(it, courseId, userId, quiz) }
            .map { toDto(it) }
            .orElseGet { createSession(courseId, quiz, userId) }
    }

    @Transactional
    fun updateSession(
        courseId: Long,
        quizId: Long,
        userId: Long,
        currentIndex: Int,
        answers: List<QuizAttemptAnswerRequest>
    ): QuizSessionDto {
        quizFacade.assertActiveQuizVisible(courseId, quizId, userId)

        val entity = quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
            .orElseThrow { QuizSessionNotFoundException.forQuizId(quizId) }
        val questionIds = deserializeQuestionIds(entity.questionIdsJson)
        val normalizedAnswers = normalizeAnswers(questionIds, answers)
        val synchronizedAnswers = synchronizeAnswers(questionIds, normalizedAnswers, loadAnswerIdsByQuestion(courseId, userId, questionIds))

        if (questionIds.isEmpty()) {
            throw IllegalArgumentException("Quiz session does not contain any playable questions.")
        }

        entity.answersJson = serializeAnswers(synchronizedAnswers)
        entity.currentIndex = max(0, min(currentIndex, questionIds.size - 1))
        entity.updatedAt = roundToDatabasePrecision(Instant.now())

        return toDto(quizSessionRepository.save(entity))
    }

    @Transactional
    fun deleteSession(courseId: Long, quizId: Long, userId: Long) {
        quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
            .ifPresent(quizSessionRepository::delete)
    }

    private fun createSession(courseId: Long, quiz: QuizSessionSpec, userId: Long): QuizSessionDto {
        val resolvedQuestions = resolveQuestionsForQuiz(courseId, userId, quiz)

        if (resolvedQuestions.questionIds.isEmpty()) {
            throw IllegalArgumentException("Quiz does not contain any playable questions.")
        }

        val answerOrderByQuestion = resolveAnswerOrderByQuestion(quiz, resolvedQuestions.answerIdsByQuestion, resolvedQuestions.questionIds)

        val now = roundToDatabasePrecision(Instant.now())
        val savedSession = quizSessionRepository.save(
            QuizSessionEntity(
                courseId = courseId,
                quizId = quiz.id,
                userId = userId,
                quizTitle = quiz.title,
                questionIdsJson = writeJson(resolvedQuestions.questionIds),
                answersJson = writeJson(emptyMap<Long, List<Long>>()),
                answerOrderJson = writeJson(answerOrderByQuestion),
                currentIndex = 0,
                createdAt = now,
                updatedAt = now
            )
        )

        return toDto(savedSession)
    }

    private fun resolveQuestionsForQuiz(courseId: Long, userId: Long, quiz: QuizSessionSpec): ResolvedSessionQuestions {
        if (quiz.mode == QuizMode.MANUAL) {
            val answerIdsByQuestion = loadAnswerIdsByQuestion(courseId, userId, quiz.questionIds)
            val availableQuestionIds = quiz.questionIds.filter { answerIdsByQuestion.containsKey(it) }
            val resolvedQuestionIds = if (quiz.questionOrder == QuizOrderMode.RANDOM) {
                availableQuestionIds.shuffled()
            } else {
                availableQuestionIds
            }

            return ResolvedSessionQuestions(
                questionIds = resolvedQuestionIds,
                answerIdsByQuestion = answerIdsByQuestion.filterKeys { resolvedQuestionIds.contains(it) }
            )
        }

        val questionSummaries = questionFacade.fetchQuestionSummaries(courseId, userId)
        val resolvedQuestionIds = resolveQuestionIdsForQuiz(quiz, questionSummaries)

        return ResolvedSessionQuestions(
            questionIds = resolvedQuestionIds,
            answerIdsByQuestion = loadAnswerIdsByQuestion(courseId, userId, resolvedQuestionIds)
        )
    }

    private fun resolveQuestionIdsForQuiz(quiz: QuizSessionSpec, questionSummaries: List<QuestionSummary>): List<Long> {
        return when (quiz.mode) {
            QuizMode.MANUAL -> {
                val availableIds = questionSummaries.map { it.id }.toSet()
                val manualQuestionIds = quiz.questionIds.filter { availableIds.contains(it) }
                if (quiz.questionOrder == QuizOrderMode.RANDOM) manualQuestionIds.shuffled() else manualQuestionIds
            }

            QuizMode.RANDOM -> {
                val courseQuestionIds = questionSummaries.map { it.id }
                val shuffled = courseQuestionIds.shuffled()
                val selected = shuffled.take(min(quiz.randomCount ?: shuffled.size, shuffled.size))
                if (quiz.questionOrder == QuizOrderMode.RANDOM) selected else courseQuestionIds.filter { selected.contains(it) }
            }

            QuizMode.CATEGORY -> {
                val categoryIds = quiz.categoryIds.toSet()
                val matchingQuestionIds = questionSummaries
                    .filter { question -> question.categoryIds.any { categoryId -> categoryIds.contains(categoryId) } }
                    .map { it.id }
                if (quiz.questionOrder == QuizOrderMode.RANDOM) {
                    matchingQuestionIds.shuffled().take(min(quiz.randomCount ?: matchingQuestionIds.size, matchingQuestionIds.size))
                } else {
                    matchingQuestionIds.take(min(quiz.randomCount ?: matchingQuestionIds.size, matchingQuestionIds.size))
                }
            }
        }
    }

    private fun normalizeAnswers(questionIds: List<Long>, answers: List<QuizAttemptAnswerRequest>): Map<Long, List<Long>> {
        if (answers.isEmpty()) {
            return emptyMap()
        }

        val allowedQuestionIds = questionIds.toSet()
        val normalized = linkedMapOf<Long, List<Long>>()

        answers.forEach { answer ->
            val questionId = answer.questionId ?: throw IllegalArgumentException("Quiz session answer question id is required.")

            if (!allowedQuestionIds.contains(questionId)) {
                throw IllegalArgumentException("Quiz session contains questions outside the selected quiz.")
            }

            if (normalized.containsKey(questionId)) {
                throw IllegalArgumentException("Quiz session cannot contain duplicate question answers.")
            }

            val selectedAnswerIds = answer.answerIds.orEmpty().fold(linkedSetOf<Long>()) { acc, answerId ->
                acc.apply { add(answerId) }
            }.toList()

            if (selectedAnswerIds.isNotEmpty()) {
                normalized[questionId] = selectedAnswerIds
            }
        }

        return normalized
    }

    private fun synchronizeSessionState(entity: QuizSessionEntity, courseId: Long, userId: Long, quiz: QuizSessionSpec): QuizSessionEntity {
        val questionIds = deserializeQuestionIds(entity.questionIdsJson)
        val answerIdsByQuestion = loadAnswerIdsByQuestion(courseId, userId, questionIds)
        val synchronizedAnswers = synchronizeAnswers(questionIds, deserializeAnswers(entity.answersJson), answerIdsByQuestion)
        val synchronizedAnswerOrder = synchronizeAnswerOrder(
            questionIds = questionIds,
            answerOrderByQuestion = deserializeAnswerOrder(entity.answerOrderJson),
            answerIdsByQuestion = answerIdsByQuestion,
            answerOrderMode = quiz.answerOrder
        )

        if (synchronizedAnswers == deserializeAnswers(entity.answersJson)
            && synchronizedAnswerOrder == deserializeAnswerOrder(entity.answerOrderJson)
        ) {
            return entity
        }

        entity.answersJson = serializeAnswers(synchronizedAnswers)
        entity.answerOrderJson = serializeAnswerOrder(synchronizedAnswerOrder)
        entity.updatedAt = roundToDatabasePrecision(Instant.now())
        return quizSessionRepository.save(entity)
    }

    private fun synchronizeAnswers(
        questionIds: List<Long>,
        answers: Map<Long, List<Long>>,
        answerIdsByQuestion: Map<Long, List<Long>>
    ): Map<Long, List<Long>> {
        if (answers.isEmpty()) {
            return emptyMap()
        }

        val synchronized = linkedMapOf<Long, List<Long>>()
        answers.forEach { (questionId, answerIds) ->
            if (!questionIds.contains(questionId)) {
                return@forEach
            }

            val availableAnswerIds = answerIdsByQuestion[questionId]
                ?: throw IllegalArgumentException("Quiz session contains questions outside the selected quiz.")
            val availableAnswerIdSet = availableAnswerIds.toSet()
            val validAnswerIds = answerIds.filter { availableAnswerIdSet.contains(it) }

            if (validAnswerIds.isNotEmpty()) {
                synchronized[questionId] = validAnswerIds
            }
        }

        return synchronized
    }

    private fun resolveAnswerOrderByQuestion(
        quiz: QuizSessionSpec,
        answerIdsByQuestion: Map<Long, List<Long>>,
        questionIds: List<Long>
    ): Map<Long, List<Long>> {
        return questionIds.associateWith { questionId ->
            val answerIds = answerIdsByQuestion[questionId]
                ?: throw IllegalArgumentException("Quiz session contains questions outside the selected quiz.")
            buildAnswerOrder(answerIds, quiz.answerOrder)
        }
    }

    private fun synchronizeAnswerOrder(
        questionIds: List<Long>,
        answerOrderByQuestion: Map<Long, List<Long>>,
        answerIdsByQuestion: Map<Long, List<Long>>,
        answerOrderMode: QuizOrderMode
    ): Map<Long, List<Long>> {
        return questionIds.associateWith { questionId ->
            val availableAnswerIds = answerIdsByQuestion[questionId]
                ?: throw IllegalArgumentException("Quiz session contains questions outside the selected quiz.")

            if (answerOrderMode != QuizOrderMode.RANDOM) {
                return@associateWith availableAnswerIds
            }

            val availableAnswerIdsSet = availableAnswerIds.toSet()
            val persistedOrder = answerOrderByQuestion[questionId].orEmpty()
                .filter { availableAnswerIdsSet.contains(it) }
                .distinct()

            if (persistedOrder.isEmpty()) {
                return@associateWith randomizeOrder(availableAnswerIds)
            }

            if (persistedOrder.size == availableAnswerIds.size && persistedOrder == availableAnswerIds) {
                return@associateWith randomizeOrder(availableAnswerIds)
            }

            val persistedOrderSet = persistedOrder.toSet()
            persistedOrder + randomizeOrder(availableAnswerIds.filterNot { persistedOrderSet.contains(it) })
        }
    }

    private fun buildAnswerOrder(answerIds: List<Long>, answerOrderMode: QuizOrderMode): List<Long> {
        return if (answerOrderMode == QuizOrderMode.RANDOM) randomizeOrder(answerIds) else answerIds
    }

    private fun randomizeOrder(itemIds: List<Long>): List<Long> {
        if (itemIds.size < 2) {
            return itemIds
        }

        val shuffled = itemIds.shuffled()
        return if (shuffled != itemIds) shuffled else itemIds.drop(1) + itemIds.first()
    }

    private fun loadAnswerIdsByQuestion(courseId: Long, userId: Long, questionIds: List<Long>): Map<Long, List<Long>> {
        return questionFacade.fetchCurrentAnswerIdsByQuestionIds(courseId, userId, questionIds)
    }

    private fun toDto(entity: QuizSessionEntity): QuizSessionDto {
        return QuizSessionDto(
            id = entity.id!!,
            courseId = entity.courseId!!,
            quizId = entity.quizId!!,
            userId = entity.userId!!,
            quizTitle = entity.quizTitle,
            questionIds = deserializeQuestionIds(entity.questionIdsJson),
            answerOrderByQuestion = deserializeAnswerOrder(entity.answerOrderJson),
            currentIndex = entity.currentIndex!!,
            answers = deserializeAnswers(entity.answersJson),
            updatedAt = entity.updatedAt
        )
    }

    private fun deserializeQuestionIds(questionIdsJson: String): List<Long> {
        return objectMapper.readValue(questionIdsJson, object : TypeReference<List<Long>>() {})
    }

    private fun serializeAnswers(answers: Map<Long, List<Long>>): String {
        return writeJson(answers)
    }

    private fun serializeAnswerOrder(answerOrderByQuestion: Map<Long, List<Long>>): String {
        return writeJson(answerOrderByQuestion)
    }

    private fun deserializeAnswers(answersJson: String): Map<Long, List<Long>> {
        return objectMapper.readValue(answersJson, object : TypeReference<Map<Long, List<Long>>>() {}) ?: emptyMap()
    }

    private fun deserializeAnswerOrder(answerOrderJson: String): Map<Long, List<Long>> {
        return objectMapper.readValue(answerOrderJson, object : TypeReference<Map<Long, List<Long>>>() {}) ?: emptyMap()
    }

    private fun writeJson(value: Any): String {
        return objectMapper.writeValueAsString(value)
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

    private fun assertCourseVisibility(courseId: Long, actorUserId: Long) {
        courseFacade.fetchCourseForMember(courseId, actorUserId)
    }

    private data class ResolvedSessionQuestions(
        val questionIds: List<Long>,
        val answerIdsByQuestion: Map<Long, List<Long>>
    )
}
