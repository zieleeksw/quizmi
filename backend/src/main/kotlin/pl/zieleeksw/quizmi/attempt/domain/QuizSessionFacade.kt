package pl.zieleeksw.quizmi.attempt.domain

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.attempt.QuizAttemptAnswerRequest
import pl.zieleeksw.quizmi.attempt.QuizSessionDto
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import pl.zieleeksw.quizmi.question.QuestionDto
import pl.zieleeksw.quizmi.question.domain.QuestionFacade
import pl.zieleeksw.quizmi.quiz.QuizDto
import pl.zieleeksw.quizmi.quiz.QuizMode
import pl.zieleeksw.quizmi.quiz.QuizOrderMode
import pl.zieleeksw.quizmi.quiz.domain.QuizFacade
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
        assertCourseVisibility(courseId)
        return quizSessionRepository.findAllByCourseIdAndUserIdOrderByUpdatedAtDesc(courseId, userId).map { toDto(it) }
    }

    @Transactional
    fun createOrResumeSession(courseId: Long, quizId: Long, userId: Long): QuizSessionDto {
        assertCourseVisibility(courseId)
        val quiz = quizFacade.fetchQuizForCourse(courseId, quizId, userId)

        return quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
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
        assertCourseVisibility(courseId)
        quizFacade.fetchQuizForCourse(courseId, quizId, userId)

        val entity = quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
            .orElseThrow { QuizSessionNotFoundException.forQuizId(quizId) }
        val questionIds = deserializeQuestionIds(entity.questionIdsJson)
        val normalizedAnswers = normalizeAnswers(questionIds, answers)
        assertAnswersBelongToQuestions(courseId, userId, normalizedAnswers)

        if (questionIds.isEmpty()) {
            throw IllegalArgumentException("Quiz session does not contain any playable questions.")
        }

        entity.answersJson = serializeAnswers(normalizedAnswers)
        entity.currentIndex = max(0, min(currentIndex, questionIds.size - 1))
        entity.updatedAt = roundToDatabasePrecision(Instant.now())

        return toDto(quizSessionRepository.save(entity))
    }

    @Transactional
    fun deleteSession(courseId: Long, quizId: Long, userId: Long) {
        quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
            .ifPresent(quizSessionRepository::delete)
    }

    private fun createSession(courseId: Long, quiz: QuizDto, userId: Long): QuizSessionDto {
        val currentQuestions = questionFacade.fetchQuestions(courseId, userId)
        val resolvedQuestionIds = resolveQuestionIdsForQuiz(quiz, currentQuestions)

        if (resolvedQuestionIds.isEmpty()) {
            throw IllegalArgumentException("Quiz does not contain any playable questions.")
        }

        val now = roundToDatabasePrecision(Instant.now())
        val savedSession = quizSessionRepository.save(
            QuizSessionEntity(
                courseId = courseId,
                quizId = quiz.id,
                userId = userId,
                quizTitle = quiz.title,
                questionIdsJson = writeJson(resolvedQuestionIds),
                answersJson = writeJson(emptyMap<Long, List<Long>>()),
                currentIndex = 0,
                createdAt = now,
                updatedAt = now
            )
        )

        return toDto(savedSession)
    }

    private fun resolveQuestionIdsForQuiz(quiz: QuizDto, currentQuestions: List<QuestionDto>): List<Long> {
        return when (quiz.mode) {
            QuizMode.MANUAL -> {
                val availableIds = currentQuestions.map { it.id }.toSet()
                val manualQuestionIds = quiz.questionIds.filter { availableIds.contains(it) }
                if (quiz.questionOrder == QuizOrderMode.RANDOM) manualQuestionIds.shuffled() else manualQuestionIds
            }

            QuizMode.RANDOM -> {
                val courseQuestionIds = currentQuestions.map { it.id }
                val shuffled = courseQuestionIds.shuffled()
                val selected = shuffled.take(min(quiz.randomCount ?: shuffled.size, shuffled.size))
                if (quiz.questionOrder == QuizOrderMode.RANDOM) selected else courseQuestionIds.filter { selected.contains(it) }
            }

            QuizMode.CATEGORY -> {
                val categoryIds = quiz.categories.map { it.id }.toSet()
                val matchingQuestionIds = currentQuestions
                    .filter { question -> question.categories.any { category -> categoryIds.contains(category.id) } }
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

    private fun assertAnswersBelongToQuestions(courseId: Long, userId: Long, answers: Map<Long, List<Long>>) {
        if (answers.isEmpty()) {
            return
        }

        val questionsById = questionFacade.fetchQuestions(courseId, userId).associateBy { it.id }

        answers.forEach { (questionId, answerIds) ->
            val question = questionsById[questionId]
                ?: throw IllegalArgumentException("Quiz session contains questions outside the selected quiz.")
            val availableAnswerIds = question.answers.map { it.id }.toSet()

            if (!availableAnswerIds.containsAll(answerIds)) {
                throw IllegalArgumentException("Quiz session contains answers outside the selected quiz.")
            }
        }
    }

    private fun toDto(entity: QuizSessionEntity): QuizSessionDto {
        return QuizSessionDto(
            id = entity.id!!,
            courseId = entity.courseId!!,
            quizId = entity.quizId!!,
            userId = entity.userId!!,
            quizTitle = entity.quizTitle,
            questionIds = deserializeQuestionIds(entity.questionIdsJson),
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

    private fun deserializeAnswers(answersJson: String): Map<Long, List<Long>> {
        return objectMapper.readValue(answersJson, object : TypeReference<Map<Long, List<Long>>>() {}) ?: emptyMap()
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

    private fun assertCourseVisibility(courseId: Long) {
        courseFacade.fetchCourseById(courseId)
    }
}
