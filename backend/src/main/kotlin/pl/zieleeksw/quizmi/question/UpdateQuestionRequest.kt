package pl.zieleeksw.quizmi.question

data class UpdateQuestionRequest(
    @field:ValidQuestionPrompt
    val prompt: String?,
    @field:ValidQuestionAnswers
    val answers: List<QuestionAnswerRequest>?,
    @field:ValidQuestionCategoryIds
    val categoryIds: List<Long>?
)
