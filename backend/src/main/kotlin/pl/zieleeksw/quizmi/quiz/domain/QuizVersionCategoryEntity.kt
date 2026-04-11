package pl.zieleeksw.quizmi.quiz.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "quiz_version_categories")
open class QuizVersionCategoryEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "quiz_version_id", nullable = false)
    open var quizVersionId: Long? = null
        protected set

    @Column(name = "category_id", nullable = false)
    open var categoryId: Long? = null
        protected set

    @Column(name = "display_order", nullable = false)
    open var displayOrder: Int? = null
        protected set

    constructor(
        quizVersionId: Long,
        categoryId: Long,
        displayOrder: Int
    ) : this() {
        this.quizVersionId = quizVersionId
        this.categoryId = categoryId
        this.displayOrder = displayOrder
    }
}
