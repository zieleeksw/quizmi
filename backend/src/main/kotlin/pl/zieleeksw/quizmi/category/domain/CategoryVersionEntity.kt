package pl.zieleeksw.quizmi.category.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "category_versions")
open class CategoryVersionEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "category_id", nullable = false)
    var categoryId: Long? = null
        protected set

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int? = null
        protected set

    @Column(nullable = false, length = 120)
    lateinit var name: String
        protected set

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant
        protected set

    constructor(
        categoryId: Long,
        versionNumber: Int,
        name: String,
        createdAt: Instant
    ) : this() {
        this.categoryId = categoryId
        this.versionNumber = versionNumber
        this.name = name
        this.createdAt = createdAt
    }
}
