package pl.zieleeksw.quizmi.category.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "categories")
open class CategoryEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "course_id", nullable = false)
    var courseId: Long? = null
        protected set

    @Column(nullable = false, length = 120)
    lateinit var name: String
        set

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant
        set

    constructor(
        courseId: Long,
        name: String,
        createdAt: Instant,
        updatedAt: Instant
    ) : this() {
        this.courseId = courseId
        this.name = name
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
}
