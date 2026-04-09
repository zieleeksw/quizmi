package pl.zieleeksw.quizmi.course.domain

class CourseNotFoundException private constructor(
    message: String
) : RuntimeException(message) {

    companion object {
        fun forId(id: Long): CourseNotFoundException {
            return CourseNotFoundException("Course with id $id was not found.")
        }
    }
}
