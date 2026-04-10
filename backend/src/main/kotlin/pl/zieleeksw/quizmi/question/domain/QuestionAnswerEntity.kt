package pl.zieleeksw.quizmi.question.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "question_answers")
open class QuestionAnswerEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "question_version_id", nullable = false)
    open var questionVersionId: Long? = null
        protected set

    @Column(name = "display_order", nullable = false)
    open var displayOrder: Int? = null
        protected set

    @Column(nullable = false, length = 300)
    open lateinit var content: String
        protected set

    @Column(nullable = false)
    open var correct: Boolean? = null
        protected set

    constructor(
        questionVersionId: Long,
        displayOrder: Int,
        content: String,
        correct: Boolean
    ) : this() {
        this.questionVersionId = questionVersionId
        this.displayOrder = displayOrder
        this.content = content
        this.correct = correct
    }
}
