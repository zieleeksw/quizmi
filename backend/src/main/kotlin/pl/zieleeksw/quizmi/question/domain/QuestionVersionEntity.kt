package pl.zieleeksw.quizmi.question.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "question_versions")
open class QuestionVersionEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "question_id", nullable = false)
    open var questionId: Long? = null
        protected set

    @Column(name = "version_number", nullable = false)
    open var versionNumber: Int? = null
        protected set

    @Column(nullable = false, length = 1000)
    open lateinit var prompt: String
        protected set

    @Column(length = 2000)
    open var explanation: String? = null
        protected set

    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant
        protected set

    constructor(
        questionId: Long,
        versionNumber: Int,
        prompt: String,
        explanation: String?,
        createdAt: Instant
    ) : this() {
        this.questionId = questionId
        this.versionNumber = versionNumber
        this.prompt = prompt
        this.explanation = explanation
        this.createdAt = createdAt
    }
}
