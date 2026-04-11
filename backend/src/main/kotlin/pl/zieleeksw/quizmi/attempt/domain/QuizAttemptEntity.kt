package pl.zieleeksw.quizmi.attempt.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "quiz_attempts")
open class QuizAttemptEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "course_id", nullable = false)
    open var courseId: Long? = null
        protected set

    @Column(name = "quiz_id", nullable = false)
    open var quizId: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    open var userId: Long? = null
        protected set

    @Column(name = "quiz_title", nullable = false, length = 120)
    open lateinit var quizTitle: String
        protected set

    @Column(name = "correct_answers", nullable = false)
    open var correctAnswers: Int? = null
        protected set

    @Column(name = "total_questions", nullable = false)
    open var totalQuestions: Int? = null
        protected set

    @Column(name = "review_snapshot_json", nullable = false, columnDefinition = "TEXT")
    open lateinit var reviewSnapshotJson: String
        protected set

    @Column(name = "finished_at", nullable = false)
    open lateinit var finishedAt: Instant
        protected set

    constructor(
        courseId: Long,
        quizId: Long,
        userId: Long,
        quizTitle: String,
        correctAnswers: Int,
        totalQuestions: Int,
        reviewSnapshotJson: String,
        finishedAt: Instant
    ) : this() {
        this.courseId = courseId
        this.quizId = quizId
        this.userId = userId
        this.quizTitle = quizTitle
        this.correctAnswers = correctAnswers
        this.totalQuestions = totalQuestions
        this.reviewSnapshotJson = reviewSnapshotJson
        this.finishedAt = finishedAt
    }
}
