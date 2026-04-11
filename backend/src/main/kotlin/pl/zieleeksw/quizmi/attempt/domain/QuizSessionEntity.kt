package pl.zieleeksw.quizmi.attempt.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "quiz_sessions")
open class QuizSessionEntity protected constructor() {

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

    @Column(name = "question_ids_json", nullable = false, columnDefinition = "TEXT")
    open lateinit var questionIdsJson: String
        set

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    open lateinit var answersJson: String
        set

    @Column(name = "current_index", nullable = false)
    open var currentIndex: Int? = null
        set

    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant
        protected set

    @Column(name = "updated_at", nullable = false)
    open lateinit var updatedAt: Instant
        set

    constructor(
        courseId: Long,
        quizId: Long,
        userId: Long,
        quizTitle: String,
        questionIdsJson: String,
        answersJson: String,
        currentIndex: Int,
        createdAt: Instant,
        updatedAt: Instant
    ) : this() {
        this.courseId = courseId
        this.quizId = quizId
        this.userId = userId
        this.quizTitle = quizTitle
        this.questionIdsJson = questionIdsJson
        this.answersJson = answersJson
        this.currentIndex = currentIndex
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
}
