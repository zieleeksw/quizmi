package pl.zieleeksw.quizmi.feedback

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.question.domain.QuestionFacade
import pl.zieleeksw.quizmi.question.domain.QuestionNotFoundException
import pl.zieleeksw.quizmi.quiz.domain.QuizFacade

@Service
class AiFeedbackFacade(
    private val quizFacade: QuizFacade,
    private val questionFacade: QuestionFacade,
    private val aiFeedbackGenerator: AiFeedbackGenerator
) {

    @Transactional(readOnly = true)
    fun generateFeedback(
        courseId: Long,
        quizId: Long,
        questionId: Long,
        userId: Long,
        selectedAnswerIds: List<Long>
    ): AiFeedbackDto {
        quizFacade.assertActiveQuizVisible(courseId, quizId, userId)
        val question = questionFacade.fetchQuestionsByIds(courseId, userId, listOf(questionId)).singleOrNull()
            ?: throw QuestionNotFoundException.forId(questionId)
        val availableAnswerIds = question.answers.map { it.id }.toSet()
        val selectedAnswerIdSet = selectedAnswerIds.toSet()

        if (!availableAnswerIds.containsAll(selectedAnswerIdSet)) {
            throw IllegalArgumentException("Selected answers do not belong to the selected question.")
        }

        val context = AiFeedbackContext(
            prompt = question.prompt,
            explanation = question.explanation,
            categories = question.categories.map { it.name },
            selectedAnswerIds = selectedAnswerIdSet,
            answers = question.answers.map { answer ->
                AiFeedbackAnswerContext(
                    id = answer.id,
                    content = answer.content,
                    correct = answer.correct,
                    selected = selectedAnswerIdSet.contains(answer.id)
                )
            }
        )

        return aiFeedbackGenerator.generate(context)
    }
}
