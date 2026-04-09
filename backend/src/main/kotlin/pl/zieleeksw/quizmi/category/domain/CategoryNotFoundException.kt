package pl.zieleeksw.quizmi.category.domain

class CategoryNotFoundException private constructor(
    message: String
) : RuntimeException(message) {

    companion object {
        fun forId(id: Long): CategoryNotFoundException {
            return CategoryNotFoundException("Category with id $id was not found.")
        }
    }
}
