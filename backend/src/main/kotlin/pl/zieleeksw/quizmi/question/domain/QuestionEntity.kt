package pl.zieleeksw.quizmi.question.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "questions")
open class QuestionEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "course_id", nullable = false)
    open var courseId: Long? = null
        protected set

    @Column(name = "current_version_number", nullable = false)
    open var currentVersionNumber: Int? = null
        set

    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant
        protected set

    @Column(name = "updated_at", nullable = false)
    open lateinit var updatedAt: Instant
        set

    constructor(
        courseId: Long,
        currentVersionNumber: Int,
        createdAt: Instant,
        updatedAt: Instant
    ) : this() {
        this.courseId = courseId
        this.currentVersionNumber = currentVersionNumber
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
}
