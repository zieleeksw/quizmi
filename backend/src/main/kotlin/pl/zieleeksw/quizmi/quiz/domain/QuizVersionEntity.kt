package pl.zieleeksw.quizmi.quiz.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import pl.zieleeksw.quizmi.quiz.QuizMode
import pl.zieleeksw.quizmi.quiz.QuizOrderMode
import java.time.Instant

@Entity
@Table(name = "quiz_versions")
open class QuizVersionEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "quiz_id", nullable = false)
    open var quizId: Long? = null
        protected set

    @Column(name = "version_number", nullable = false)
    open var versionNumber: Int? = null
        protected set

    @Column(nullable = false, length = 120)
    open lateinit var title: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    open var mode: QuizMode? = null
        protected set

    @Column(name = "random_count")
    open var randomCount: Int? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "question_order", nullable = false, length = 16)
    open var questionOrder: QuizOrderMode? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_order", nullable = false, length = 16)
    open var answerOrder: QuizOrderMode? = null
        protected set

    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant
        protected set

    constructor(
        quizId: Long,
        versionNumber: Int,
        title: String,
        mode: QuizMode,
        randomCount: Int?,
        questionOrder: QuizOrderMode,
        answerOrder: QuizOrderMode,
        createdAt: Instant
    ) : this() {
        this.quizId = quizId
        this.versionNumber = versionNumber
        this.title = title
        this.mode = mode
        this.randomCount = randomCount
        this.questionOrder = questionOrder
        this.answerOrder = answerOrder
        this.createdAt = createdAt
    }
}
