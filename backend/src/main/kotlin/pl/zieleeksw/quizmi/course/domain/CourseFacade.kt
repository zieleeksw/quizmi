package pl.zieleeksw.quizmi.course.domain

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.zieleeksw.quizmi.course.CourseDto
import pl.zieleeksw.quizmi.course.CourseMemberDto
import pl.zieleeksw.quizmi.course.CourseMembersDto
import pl.zieleeksw.quizmi.course.CourseMembershipRole
import pl.zieleeksw.quizmi.course.CourseMembershipStatus
import pl.zieleeksw.quizmi.user.domain.UserEntity
import pl.zieleeksw.quizmi.user.domain.UserRepository
import pl.zieleeksw.quizmi.user.domain.UserRole
import java.time.Instant

@Service
class CourseFacade(
    private val courseRepository: CourseRepository,
    private val courseMembershipRepository: CourseMembershipRepository,
    private val userRepository: UserRepository,
    private val courseNameValidator: CourseNameValidator,
    private val courseDescriptionValidator: CourseDescriptionValidator
) {

    @Transactional
    fun createCourse(
        name: String,
        description: String,
        ownerUserId: Long
    ): CourseDto {
        val normalizedName = name.trim()
        val normalizedDescription = description.trim()

        courseNameValidator.validate(normalizedName)
        courseDescriptionValidator.validate(normalizedDescription)

        val savedCourse = courseRepository.save(
            CourseEntity(
                name = normalizedName,
                description = normalizedDescription,
                createdAt = Instant.now(),
                ownerUserId = ownerUserId
            )
        )

        val now = savedCourse.createdAt
        courseMembershipRepository.save(
            CourseMembershipEntity(
                courseId = savedCourse.id!!,
                userId = ownerUserId,
                role = CourseMembershipRole.OWNER,
                status = CourseMembershipStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        )

        return toCourseDto(
            course = savedCourse,
            actor = findUserOrThrow(ownerUserId),
            membership = courseMembershipRepository.findByCourseIdAndUserId(savedCourse.id!!, ownerUserId).orElse(null),
            ownerEmail = readOwnerEmail(savedCourse.ownerUserId!!)
        )
    }

    @Transactional(readOnly = true)
    fun fetchVisibleCourses(actorUserId: Long): List<CourseDto> {
        val actor = findUserOrThrow(actorUserId)
        val courses = courseRepository.findAllByOrderByCreatedAtDesc()
        val courseIds = courses.mapNotNull { it.id }
        val ownersById = userRepository.findAllById(courses.mapNotNull { it.ownerUserId }.distinct())
            .associateBy { it.id!! }
        val membershipsByCourseId = courseMembershipRepository.findAllByUserIdAndCourseIdIn(actorUserId, courseIds)
            .associateBy { it.courseId!! }

        return courses.map { course ->
            val ownerEmail = ownersById[course.ownerUserId!!]?.email
                ?: throw IllegalStateException("Owner with id ${course.ownerUserId} was not found for course ${course.id}.")

            toCourseDto(
                course = course,
                actor = actor,
                membership = membershipsByCourseId[course.id!!],
                ownerEmail = ownerEmail
            )
        }
    }

    @Transactional(readOnly = true)
    fun fetchCourseById(
        id: Long,
        actorUserId: Long
    ): CourseDto {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)

        return toCourseDto(
            course = entity,
            actor = actor,
            membership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null),
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    @Transactional(readOnly = true)
    fun assertCourseExists(id: Long) {
        courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
    }

    @Transactional(readOnly = true)
    fun hasCourseAccess(
        id: Long,
        actorUserId: Long
    ): Boolean {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)

        return resolveAccess(entity, actor, membership).canAccess
    }

    @Transactional(readOnly = true)
    fun fetchCourseForMember(
        id: Long,
        actorUserId: Long
    ): CourseDto {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        val access = resolveAccess(entity, actor, membership)

        if (!access.canAccess) {
            throw AccessDeniedException("You do not have access to this course.")
        }

        return toCourseDto(
            course = entity,
            actor = actor,
            membership = membership,
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    @Transactional(readOnly = true)
    fun fetchCourseForManager(
        id: Long,
        actorUserId: Long
    ): CourseDto {
        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        val access = resolveAccess(entity, actor, membership)

        if (!access.canManage) {
            throw AccessDeniedException("You cannot manage this course.")
        }

        return toCourseDto(
            course = entity,
            actor = actor,
            membership = membership,
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    @Transactional(readOnly = true)
    fun fetchCourseMembers(
        id: Long,
        actorUserId: Long
    ): CourseMembersDto {
        val course = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val actorMembership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        assertCanManageCourse(course, actor, actorMembership)

        val activeMemberships = courseMembershipRepository.findAllByCourseIdAndStatus(id, CourseMembershipStatus.ACTIVE)
        val pendingMemberships = courseMembershipRepository.findAllByCourseIdAndStatus(id, CourseMembershipStatus.PENDING)
        val usersById = userRepository.findAllById((activeMemberships + pendingMemberships).mapNotNull { it.userId }.distinct())
            .associateBy { it.id!! }

        return CourseMembersDto(
            members = activeMemberships
                .map { it.toMemberDto(usersById) }
                .sortedWith(compareBy<CourseMemberDto> { rolePriority(it.role) }.thenBy { it.email.lowercase() }),
            pendingRequests = pendingMemberships
                .map { it.toMemberDto(usersById) }
                .sortedBy { it.email.lowercase() }
        )
    }

    @Transactional
    fun requestToJoinCourse(
        id: Long,
        actorUserId: Long
    ): CourseDto {
        val course = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val existingMembership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        val access = resolveAccess(course, actor, existingMembership)

        if (access.canAccess) {
            throw IllegalArgumentException("You already have access to this course.")
        }

        if (existingMembership?.status == CourseMembershipStatus.PENDING) {
            throw IllegalArgumentException("Your join request is already pending for this course.")
        }

        val now = Instant.now()
        courseMembershipRepository.save(
            CourseMembershipEntity(
                courseId = id,
                userId = actorUserId,
                role = CourseMembershipRole.MEMBER,
                status = CourseMembershipStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )

        return toCourseDto(
            course = course,
            actor = actor,
            membership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null),
            ownerEmail = readOwnerEmail(course.ownerUserId!!)
        )
    }

    @Transactional
    fun approveJoinRequest(
        id: Long,
        memberUserId: Long,
        actorUserId: Long
    ): CourseMemberDto {
        val course = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val actorMembership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        assertCanManageCourse(course, actor, actorMembership)

        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, memberUserId)
            .orElseThrow { IllegalArgumentException("Course join request was not found.") }

        if (membership.status != CourseMembershipStatus.PENDING) {
            throw IllegalArgumentException("Only pending requests can be approved.")
        }

        membership.status = CourseMembershipStatus.ACTIVE
        membership.role = CourseMembershipRole.MEMBER
        membership.updatedAt = Instant.now()

        val savedMembership = courseMembershipRepository.save(membership)
        val usersById = userRepository.findAllById(listOf(memberUserId)).associateBy { it.id!! }

        return savedMembership.toMemberDto(usersById)
    }

    @Transactional
    fun declineJoinRequest(
        id: Long,
        memberUserId: Long,
        actorUserId: Long
    ) {
        val course = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val actorMembership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        assertCanManageCourse(course, actor, actorMembership)

        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, memberUserId)
            .orElseThrow { IllegalArgumentException("Course join request was not found.") }

        if (membership.status != CourseMembershipStatus.PENDING) {
            throw IllegalArgumentException("Only pending requests can be declined.")
        }

        courseMembershipRepository.delete(membership)
    }

    @Transactional
    fun removeCourseMember(
        id: Long,
        memberUserId: Long,
        actorUserId: Long
    ) {
        val course = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val actorMembership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, memberUserId)
            .orElseThrow { IllegalArgumentException("Course member was not found.") }

        assertCanRemoveMember(course, actor, actorMembership, membership)
        courseMembershipRepository.delete(membership)
    }

    @Transactional
    fun updateCourseMemberRole(
        id: Long,
        memberUserId: Long,
        role: CourseMembershipRole,
        actorUserId: Long
    ): CourseMemberDto {
        val course = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val actorMembership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        val access = resolveAccess(course, actor, actorMembership)

        if (!access.canChangeRoles) {
            throw AccessDeniedException("Only the course owner can change member roles.")
        }

        if (role == CourseMembershipRole.OWNER) {
            throw IllegalArgumentException("Owner role cannot be assigned manually.")
        }

        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, memberUserId)
            .orElseThrow { IllegalArgumentException("Course member was not found.") }

        assertCanUpdateMemberRole(course, actor, actorMembership, membership, role)

        membership.role = role
        membership.updatedAt = Instant.now()

        val savedMembership = courseMembershipRepository.save(membership)
        val usersById = userRepository.findAllById(listOf(memberUserId)).associateBy { it.id!! }

        return savedMembership.toMemberDto(usersById)
    }

    @Transactional
    fun updateCourse(
        id: Long,
        name: String,
        description: String,
        actorUserId: Long
    ): CourseDto {
        val normalizedName = name.trim()
        val normalizedDescription = description.trim()

        courseNameValidator.validate(normalizedName)
        courseDescriptionValidator.validate(normalizedDescription)

        val entity = courseRepository.findById(id)
            .orElseThrow { CourseNotFoundException.forId(id) }
        val actor = findUserOrThrow(actorUserId)
        val membership = courseMembershipRepository.findByCourseIdAndUserId(id, actorUserId).orElse(null)
        val access = resolveAccess(entity, actor, membership)

        if (!access.canManage) {
            throw AccessDeniedException("You cannot manage this course.")
        }

        entity.name = normalizedName
        entity.description = normalizedDescription

        return toCourseDto(
            course = courseRepository.save(entity),
            actor = actor,
            membership = membership,
            ownerEmail = readOwnerEmail(entity.ownerUserId!!)
        )
    }

    private fun findUserOrThrow(userId: Long): UserEntity {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User with id $userId was not found.") }
    }

    private fun readOwnerEmail(ownerUserId: Long): String {
        return userRepository.findById(ownerUserId)
            .map(UserEntity::email)
            .orElseThrow { IllegalStateException("Owner with id $ownerUserId was not found.") }
    }

    private fun toCourseDto(
        course: CourseEntity,
        actor: UserEntity,
        membership: CourseMembershipEntity?,
        ownerEmail: String
    ): CourseDto {
        val access = resolveAccess(course, actor, membership)

        return CourseDto(
            id = course.id!!,
            name = course.name,
            description = course.description,
            createdAt = course.createdAt,
            ownerUserId = course.ownerUserId!!,
            ownerEmail = ownerEmail,
            membershipRole = access.membershipRole,
            membershipStatus = access.membershipStatus,
            canAccess = access.canAccess,
            canManage = access.canManage,
            pendingRequestsCount = if (access.canManage) {
                courseMembershipRepository.countByCourseIdAndStatus(course.id!!, CourseMembershipStatus.PENDING).toInt()
            } else {
                0
            }
        )
    }

    private fun resolveAccess(
        course: CourseEntity,
        actor: UserEntity,
        membership: CourseMembershipEntity?
    ): CourseAccess {
        if (actor.role == UserRole.ADMIN) {
            return CourseAccess(
                membershipRole = membership?.role,
                membershipStatus = membership?.status,
                canAccess = true,
                canManage = true,
                canChangeRoles = true
            )
        }

        val membershipRole = when {
            course.ownerUserId == actor.id -> CourseMembershipRole.OWNER
            else -> membership?.role
        }
        val membershipStatus = when {
            course.ownerUserId == actor.id -> CourseMembershipStatus.ACTIVE
            else -> membership?.status
        }
        val isActiveMember = membershipStatus == CourseMembershipStatus.ACTIVE
        val canManage = isActiveMember && membershipRole in setOf(CourseMembershipRole.OWNER, CourseMembershipRole.MODERATOR)

        return CourseAccess(
            membershipRole = membershipRole,
            membershipStatus = membershipStatus,
            canAccess = isActiveMember,
            canManage = canManage,
            canChangeRoles = isActiveMember && membershipRole == CourseMembershipRole.OWNER
        )
    }

    private fun assertCanManageCourse(
        course: CourseEntity,
        actor: UserEntity,
        membership: CourseMembershipEntity?
    ) {
        if (actor.role == UserRole.ADMIN) {
            return
        }

        val isOwner = course.ownerUserId == actor.id
        val isActiveManager = membership?.status == CourseMembershipStatus.ACTIVE &&
            membership.role in setOf(CourseMembershipRole.OWNER, CourseMembershipRole.MODERATOR)

        if (!isOwner && !isActiveManager) {
            throw AccessDeniedException("You cannot manage this course.")
        }
    }

    private fun assertCanUpdateMemberRole(
        course: CourseEntity,
        actor: UserEntity,
        actorMembership: CourseMembershipEntity?,
        targetMembership: CourseMembershipEntity,
        desiredRole: CourseMembershipRole
    ) {
        if (targetMembership.status != CourseMembershipStatus.ACTIVE) {
            throw IllegalArgumentException("Only active course members can receive a role update.")
        }

        if (targetMembership.role == CourseMembershipRole.OWNER || course.ownerUserId == targetMembership.userId) {
            throw IllegalArgumentException("The course owner role cannot be changed.")
        }

        if (actor.role == UserRole.ADMIN) {
            return
        }

        if (course.ownerUserId == actor.id) {
            return
        }

        val isModerator = actorMembership?.status == CourseMembershipStatus.ACTIVE &&
            actorMembership.role == CourseMembershipRole.MODERATOR

        if (!isModerator) {
            throw AccessDeniedException("You cannot manage this course.")
        }

        if (desiredRole != CourseMembershipRole.MEMBER) {
            throw AccessDeniedException("Moderators cannot promote members to moderator.")
        }

        if (targetMembership.role != CourseMembershipRole.MODERATOR) {
            throw AccessDeniedException("Moderators can only demote moderators to members.")
        }
    }

    private fun assertCanRemoveMember(
        course: CourseEntity,
        actor: UserEntity,
        actorMembership: CourseMembershipEntity?,
        targetMembership: CourseMembershipEntity
    ) {
        if (targetMembership.status != CourseMembershipStatus.ACTIVE) {
            throw IllegalArgumentException("Only active course members can be removed.")
        }

        if (targetMembership.role == CourseMembershipRole.OWNER || course.ownerUserId == targetMembership.userId) {
            throw IllegalArgumentException("The course owner cannot be removed.")
        }

        if (actor.role == UserRole.ADMIN) {
            return
        }

        if (course.ownerUserId == actor.id) {
            return
        }

        val isModerator = actorMembership?.status == CourseMembershipStatus.ACTIVE &&
            actorMembership.role == CourseMembershipRole.MODERATOR

        if (!isModerator) {
            throw AccessDeniedException("You cannot manage this course.")
        }
    }

    private fun CourseMembershipEntity.toMemberDto(usersById: Map<Long, UserEntity>): CourseMemberDto {
        val userId = userId ?: throw IllegalStateException("Course membership user id is missing.")
        val email = usersById[userId]?.email
            ?: throw IllegalStateException("User with id $userId was not found.")

        return CourseMemberDto(
            userId = userId,
            email = email,
            role = role,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun rolePriority(role: CourseMembershipRole): Int {
        return when (role) {
            CourseMembershipRole.OWNER -> 0
            CourseMembershipRole.MODERATOR -> 1
            CourseMembershipRole.MEMBER -> 2
        }
    }

    private data class CourseAccess(
        val membershipRole: CourseMembershipRole?,
        val membershipStatus: CourseMembershipStatus?,
        val canAccess: Boolean,
        val canManage: Boolean,
        val canChangeRoles: Boolean
    )
}
