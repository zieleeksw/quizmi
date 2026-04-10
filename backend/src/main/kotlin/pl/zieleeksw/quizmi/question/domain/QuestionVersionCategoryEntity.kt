package pl.zieleeksw.quizmi.question.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "question_version_categories")
open class QuestionVersionCategoryEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "question_version_id", nullable = false)
    open var questionVersionId: Long? = null
        protected set

    @Column(name = "category_id", nullable = false)
    open var categoryId: Long? = null
        protected set

    @Column(name = "display_order", nullable = false)
    open var displayOrder: Int? = null
        protected set

    constructor(
        questionVersionId: Long,
        categoryId: Long,
        displayOrder: Int
    ) : this() {
        this.questionVersionId = questionVersionId
        this.categoryId = categoryId
        this.displayOrder = displayOrder
    }
}
