package pl.zieleeksw.quizmi.question

data class CreateQuestionRequest(
    @field:ValidQuestionPrompt
    val prompt: String?,
    @field:ValidQuestionExplanation
    val explanation: String?,
    @field:ValidQuestionAnswers
    val answers: List<QuestionAnswerRequest>?,
    @field:ValidQuestionCategoryIds
    val categoryIds: List<Long>?
)
