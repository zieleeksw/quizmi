package pl.zieleeksw.quizmi.category

data class UpdateCategoryRequest(
    @field:ValidCategoryName
    val name: String?
)
