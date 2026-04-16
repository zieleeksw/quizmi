package pl.zieleeksw.quizmi.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import pl.zieleeksw.quizmi.course.CourseMembershipRole
import pl.zieleeksw.quizmi.course.CourseMembershipStatus
import java.time.Instant

@Entity
@Table(
    name = "course_memberships",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_course_memberships_course_user", columnNames = ["course_id", "user_id"])
    ]
)
open class CourseMembershipEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @Column(name = "course_id", nullable = false)
    open var courseId: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    open var userId: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    open lateinit var role: CourseMembershipRole
        set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    open lateinit var status: CourseMembershipStatus
        set

    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant
        protected set

    @Column(name = "updated_at", nullable = false)
    open lateinit var updatedAt: Instant
        set

    constructor(
        courseId: Long,
        userId: Long,
        role: CourseMembershipRole,
        status: CourseMembershipStatus,
        createdAt: Instant,
        updatedAt: Instant
    ) : this() {
        this.courseId = courseId
        this.userId = userId
        this.role = role
        this.status = status
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
}
