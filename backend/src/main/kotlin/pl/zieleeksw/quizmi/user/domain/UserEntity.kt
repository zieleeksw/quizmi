package pl.zieleeksw.quizmi.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_email", columnNames = ["email"])
    ]
)
open class UserEntity protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, length = 255, unique = true)
    lateinit var email: String
        protected set

    @Column(name = "password_hash", nullable = false, length = 255)
    lateinit var passwordHash: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    lateinit var role: UserRole
        protected set

    constructor(
        email: String,
        passwordHash: String,
        role: UserRole
    ) : this() {
        this.email = email
        this.passwordHash = passwordHash
        this.role = role
    }
}
