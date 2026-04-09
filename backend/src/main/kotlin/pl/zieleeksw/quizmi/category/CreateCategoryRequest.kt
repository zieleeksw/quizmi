package pl.zieleeksw.quizmi.category

data class CreateCategoryRequest(
    @field:ValidCategoryName
    val name: String?
)
