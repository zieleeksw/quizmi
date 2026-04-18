package pl.zieleeksw.quizmi.attempt.domain

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.attempt.QuizAttemptAnswerReviewDto
import pl.zieleeksw.quizmi.attempt.QuizAttemptAnswerRequest
import pl.zieleeksw.quizmi.attempt.QuizAttemptDetailDto
import pl.zieleeksw.quizmi.attempt.QuizAttemptDto
import pl.zieleeksw.quizmi.attempt.QuizAttemptQuestionReviewDto
import pl.zieleeksw.quizmi.course.domain.CourseFacade
import pl.zieleeksw.quizmi.question.QuestionDto
import pl.zieleeksw.quizmi.question.domain.QuestionFacade
import pl.zieleeksw.quizmi.quiz.QuizDto
import pl.zieleeksw.quizmi.quiz.QuizMode
import pl.zieleeksw.quizmi.quiz.domain.QuizFacade
import java.time.Instant

@Service
class QuizAttemptFacade(
    private val quizAttemptRepository: QuizAttemptRepository,
    private val quizSessionRepository: QuizSessionRepository,
    private val courseFacade: CourseFacade,
    private val quizFacade: QuizFacade,
    private val questionFacade: QuestionFacade,
    private val objectMapper: ObjectMapper
) {

    @Transactional(readOnly = true)
    fun fetchCourseAttempts(courseId: Long, userId: Long): List<QuizAttemptDto> {
        assertCourseVisibility(courseId, userId)
        return quizAttemptRepository.findAllByCourseIdAndUserIdOrderByFinishedAtDesc(courseId, userId).map { toDto(it) }
    }

    @Transactional(readOnly = true)
    fun fetchAttemptDetail(courseId: Long, attemptId: Long, userId: Long): QuizAttemptDetailDto {
        assertCourseVisibility(courseId, userId)
        val entity = quizAttemptRepository.findByIdAndCourseIdAndUserId(attemptId, courseId, userId)
            .orElseThrow { QuizAttemptNotFoundException.forId(attemptId) }

        return toDetailDto(entity)
    }

    @Transactional(readOnly = true)
    fun fetchCourseAttemptReviews(courseId: Long, userId: Long): List<QuizAttemptDetailDto> {
        assertCourseVisibility(courseId, userId)
        return quizAttemptRepository.findAllByCourseIdAndUserIdOrderByFinishedAtDesc(courseId, userId).map { toDetailDto(it) }
    }

    @Transactional
    fun createAttempt(courseId: Long, quizId: Long, userId: Long, answers: List<QuizAttemptAnswerRequest>): QuizAttemptDto {
        assertCourseVisibility(courseId, userId)
        val quiz = quizFacade.fetchQuizForCourse(courseId, quizId, userId)
        val submittedAnswers = normalizeSubmittedAnswers(answers)
        val currentQuestions = questionFacade.fetchQuestions(courseId, userId)
        val questionsById = currentQuestions.associateBy { it.id }
        val activeSession = quizSessionRepository.findByCourseIdAndQuizIdAndUserId(courseId, quizId, userId)
        val questionSpec = activeSession
            .map {
                val sessionQuestionIds = deserializeQuestionIds(it.questionIdsJson)
                AttemptQuestionSpec(sessionQuestionIds.toSet(), sessionQuestionIds.size, sessionQuestionIds)
            }
            .orElseGet { resolveQuestionSpec(quiz, currentQuestions) }

        assertSubmittedQuestionsMatchQuiz(submittedAnswers.keys, questionSpec)
        val synchronizedSubmittedAnswers = synchronizeSubmittedAnswers(submittedAnswers, questionsById)

        val reviewSnapshot = buildReviewSnapshot(questionSpec.orderedQuestionIds, synchronizedSubmittedAnswers, questionsById)
        val correctAnswers = reviewSnapshot.count { it.answeredCorrectly }
        val finishedAt = roundToDatabasePrecision(Instant.now())
        val savedAttempt = quizAttemptRepository.save(
            QuizAttemptEntity(
                courseId = courseId,
                quizId = quiz.id,
                userId = userId,
                quizTitle = activeSession.map { it.quizTitle }.orElse(quiz.title),
                correctAnswers = correctAnswers,
                totalQuestions = questionSpec.expectedCount,
                reviewSnapshotJson = objectMapper.writeValueAsString(reviewSnapshot),
                finishedAt = finishedAt
            )
        )

        activeSession.ifPresent(quizSessionRepository::delete)
        return toDto(savedAttempt)
    }

    private fun normalizeSubmittedAnswers(answers: List<QuizAttemptAnswerRequest>): Map<Long, Set<Long>> {
        val submittedAnswers = linkedMapOf<Long, Set<Long>>()

        answers.forEach { answer ->
            val questionId = answer.questionId ?: throw IllegalArgumentException("Quiz attempt answer question id is required.")
            val normalizedAnswerIds = answer.answerIds.orEmpty().toSet()

            if (submittedAnswers.putIfAbsent(questionId, normalizedAnswerIds) != null) {
                throw IllegalArgumentException("Quiz attempt cannot contain duplicate question answers.")
            }
        }

        return submittedAnswers
    }

    private fun resolveQuestionSpec(quiz: QuizDto, currentQuestions: List<QuestionDto>): AttemptQuestionSpec {
        return when (quiz.mode) {
            QuizMode.MANUAL -> AttemptQuestionSpec(quiz.questionIds.toSet(), quiz.questionIds.size, quiz.questionIds)
            QuizMode.RANDOM -> {
                val questionIds = currentQuestions.map { it.id }
                AttemptQuestionSpec(questionIds.toSet(), minOf(quiz.randomCount ?: questionIds.size, questionIds.size), questionIds)
            }

            QuizMode.CATEGORY -> {
                val categoryIds = quiz.categories.map { it.id }.toSet()
                val questionIds = currentQuestions
                    .filter { question -> question.categories.any { category -> categoryIds.contains(category.id) } }
                    .map { it.id }
                val expectedCount = minOf(quiz.randomCount ?: questionIds.size, questionIds.size)
                AttemptQuestionSpec(questionIds.toSet(), expectedCount, questionIds.take(expectedCount))
            }
        }
    }

    private fun assertSubmittedQuestionsMatchQuiz(submittedQuestionIds: Set<Long>, questionSpec: AttemptQuestionSpec) {
        if (!questionSpec.allowedQuestionIds.containsAll(submittedQuestionIds)) {
            throw IllegalArgumentException("Quiz attempt contains questions outside the selected quiz.")
        }
    }

    private fun synchronizeSubmittedAnswers(
        submittedAnswers: Map<Long, Set<Long>>,
        questionsById: Map<Long, QuestionDto>
    ): Map<Long, Set<Long>> {
        if (submittedAnswers.isEmpty()) {
            return emptyMap()
        }

        val synchronizedAnswers = linkedMapOf<Long, Set<Long>>()

        submittedAnswers.forEach { (questionId, answerIds) ->
            val question = questionsById[questionId] ?: return@forEach
            val availableAnswerIds = question.answers.map { it.id }.toSet()
            synchronizedAnswers[questionId] = answerIds.filter { availableAnswerIds.contains(it) }.toSet()
        }

        return synchronizedAnswers
    }

    private fun buildReviewSnapshot(
        orderedQuestionIds: List<Long>,
        submittedAnswers: Map<Long, Set<Long>>,
        questionsById: Map<Long, QuestionDto>
    ): List<ReviewQuestionSnapshot> {
        return orderedQuestionIds
            .map { questionId ->
                val question = questionsById[questionId]
                    ?: throw IllegalArgumentException("Quiz attempt contains questions outside the selected quiz.")
                val selectedAnswerIds = submittedAnswers[questionId] ?: emptySet()
                val availableAnswerIds = question.answers.map { it.id }.toSet()

                if (!availableAnswerIds.containsAll(selectedAnswerIds)) {
                    throw IllegalArgumentException("Submitted answers do not belong to the selected question.")
                }

                val correctAnswerIds = question.answers.filter { it.correct }.map { it.id }.toSet()

                ReviewQuestionSnapshot(
                    questionId = question.id,
                    prompt = question.prompt,
                    explanation = question.explanation,
                    selectedAnswerIds = selectedAnswerIds.toList(),
                    correctAnswerIds = correctAnswerIds.toList(),
                    answeredCorrectly = selectedAnswerIds == correctAnswerIds,
                    answers = question.answers.map { answer ->
                        ReviewAnswerSnapshot(answer.id, answer.displayOrder, answer.content, answer.correct)
                    }
                )
            }
    }

    private fun toDto(entity: QuizAttemptEntity): QuizAttemptDto {
        return QuizAttemptDto(
            id = entity.id!!,
            courseId = entity.courseId!!,
            quizId = entity.quizId!!,
            userId = entity.userId!!,
            quizTitle = entity.quizTitle,
            correctAnswers = entity.correctAnswers!!,
            totalQuestions = entity.totalQuestions!!,
            finishedAt = entity.finishedAt
        )
    }

    private fun toDetailDto(entity: QuizAttemptEntity): QuizAttemptDetailDto {
        val review = objectMapper.readValue(entity.reviewSnapshotJson, object : TypeReference<List<ReviewQuestionSnapshot>>() {})

        return QuizAttemptDetailDto(
            id = entity.id!!,
            courseId = entity.courseId!!,
            quizId = entity.quizId!!,
            userId = entity.userId!!,
            quizTitle = entity.quizTitle,
            correctAnswers = entity.correctAnswers!!,
            totalQuestions = entity.totalQuestions!!,
            finishedAt = entity.finishedAt,
            questions = review.map { snapshot ->
                QuizAttemptQuestionReviewDto(
                    questionId = snapshot.questionId,
                    prompt = snapshot.prompt,
                    explanation = snapshot.explanation,
                    selectedAnswerIds = snapshot.selectedAnswerIds,
                    correctAnswerIds = snapshot.correctAnswerIds,
                    answeredCorrectly = snapshot.answeredCorrectly,
                    answers = snapshot.answers.map { answer ->
                        QuizAttemptAnswerReviewDto(answer.id, answer.displayOrder, answer.content, answer.correct)
                    }
                )
            }
        )
    }

    private fun deserializeQuestionIds(questionIdsJson: String): List<Long> {
        return objectMapper.readValue(questionIdsJson, object : TypeReference<List<Long>>() {})
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

    private data class AttemptQuestionSpec(
        val allowedQuestionIds: Set<Long>,
        val expectedCount: Int,
        val orderedQuestionIds: List<Long>
    )

    private data class ReviewQuestionSnapshot(
        val questionId: Long,
        val prompt: String,
        val explanation: String?,
        val selectedAnswerIds: List<Long>,
        val correctAnswerIds: List<Long>,
        val answeredCorrectly: Boolean,
        val answers: List<ReviewAnswerSnapshot>
    )

    private data class ReviewAnswerSnapshot(
        val id: Long,
        val displayOrder: Int,
        val content: String,
        val correct: Boolean
    )
}
