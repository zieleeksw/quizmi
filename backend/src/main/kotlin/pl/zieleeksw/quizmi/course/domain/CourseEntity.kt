package pl.zieleeksw.quizmi.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "courses")
open class CourseEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(nullable = false, length = 120)
    open lateinit var name: String
        set

    @Column(nullable = false, length = 1000)
    open lateinit var description: String
        set

    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant
        protected set

    @Column(name = "owner_user_id", nullable = false)
    open var ownerUserId: Long? = null
        protected set

    constructor(
        name: String,
        description: String,
        createdAt: Instant,
        ownerUserId: Long
    ) : this() {
        this.name = name
        this.description = description
        this.createdAt = createdAt
        this.ownerUserId = ownerUserId
    }
}
