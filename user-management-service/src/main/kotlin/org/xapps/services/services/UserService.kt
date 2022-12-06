package org.xapps.services.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.elytron.security.common.BcryptUtil
import org.xapps.services.entities.Authentication
import org.xapps.services.entities.Credential
import org.xapps.services.entities.Role
import org.xapps.services.entities.User
import org.xapps.services.repositories.RoleRepository
import org.xapps.services.repositories.UserRepository
import org.xapps.services.services.exceptions.InvalidInputDataException
import org.xapps.services.services.exceptions.UserNotFoundException
import org.xapps.services.services.exceptions.UsernameNotAvailableException
import org.xapps.services.utils.TokenUtils
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.transaction.Transactional

@ApplicationScoped
class UserService @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val tokenUtils: TokenUtils,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    fun login(credential: Credential): Authentication? =
        userRepository.findByUsername(credential.username)?.let { user ->
            if (BcryptUtil.matches(credential.password, user.password)) {
                val groups: Set<String>? = user.roles?.map { it.value }?.filterNotNull()?.toSet()
                Authentication(
                        token = tokenUtils.generateToken(user.username, groups, user)
                )
            } else {
                null
            }
        }

    fun buildUser(json: String): User =
        objectMapper.readValue(json, User::class.java)

    fun isAdministrator(user: User): Boolean =
        user.roles?.any { role -> role.value == Role.ADMINISTRATOR } == true

    fun hasAdministratorRole(user: User?): Boolean {
        var has = false
        if (user?.roles?.isNotEmpty() == true) {
            val adminRole: Role? = roleRepository.findByValue(Role.ADMINISTRATOR)!!
            has = adminRole != null && user.roles!!.any { role -> role.id == adminRole.id }
        }
        return has
    }
    fun findAll(): List<User> =
            userRepository.findAll().list()

    fun findById(id: Long): User? =
            userRepository.findById(id)

    fun findAllRoles(): List<Role> =
            roleRepository.findAll().list()

    @Transactional
    fun create(user: User): User {
        return if(user.username != null && user.password != null) {
            val duplicity = userRepository.findByUsername(user.username!!)
            if(duplicity == null) {
                user.password = BcryptUtil.bcryptHash(user.password)
                var roles: List<Role>? = user.roles?.let {
                    it.mapNotNull { role -> roleRepository.findById(role.id) }
                }
                if (user.roles == null || roles?.isEmpty() == true) {
                    val guestRole: Role = roleRepository.findByValue(Role.GUEST)!!
                    roles = listOf(guestRole)
                }
                user.roles = roles
                userRepository.persist(user)
                user
            } else {
                throw UsernameNotAvailableException()
            }
        } else {
            throw InvalidInputDataException("Username and password cannot be empty")
        }
    }

    @Transactional
    fun update(id: Long, user: User): User =
        userRepository.findById(id)?.let { persistedUser ->
            val duplicity = user.username?.let { checkedNewUsername ->
                userRepository.findByIdNotAndUsername(id, checkedNewUsername) != null
            } ?: run { false }
            if (duplicity) {
                throw UsernameNotAvailableException()
            } else {
                persistedUser.roles.toString()
                user.username?.let { newUsername ->
                    persistedUser.username = newUsername
                }
                user.password?.let { newPassword ->
                    persistedUser.password = BcryptUtil.bcryptHash(newPassword)
                }

                if (user.roles != null && user.roles?.isNotEmpty() == true) {
                    val roles: List<Role> = roleRepository.findByIds(user.roles!!.map { role -> role.id })
                    if (roles.isNotEmpty()) {
                        persistedUser.roles = roles
                    }
                }
                userRepository.persist(persistedUser)
                persistedUser
            }
        } ?: run {
            throw UserNotFoundException()
        }

    @Transactional
    fun delete(id: Long): Boolean =
        userRepository.findById(id)?.let {
            userRepository.deleteById(id)
        } ?: run {
            throw UserNotFoundException()
        }


}