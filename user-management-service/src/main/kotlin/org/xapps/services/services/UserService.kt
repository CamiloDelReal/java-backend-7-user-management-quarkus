package org.xapps.services.services

import io.quarkus.elytron.security.common.BcryptUtil
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.xapps.services.entities.Authentication
import org.xapps.services.entities.Credential
import org.xapps.services.entities.User
import org.xapps.services.repositories.UserRepository
import org.xapps.services.utils.TokenUtils
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.transaction.Transactional

@ApplicationScoped
class UserService @Inject constructor(
    private val tokenUtils: TokenUtils,
    private val userRepository: UserRepository,
    @ConfigProperty(name = "security.token.issuer")
    private val tokenIssuer: String,
    @ConfigProperty(name = "security.token.expiration")
    private val tokenExpiration: Long
) {

    fun login(credential: Credential): Authentication? =
        userRepository.findByUsername(credential.username)?.let { user ->
            if(BcryptUtil.matches(credential.password, user.password)) {
                val issuedTime = Instant.now().epochSecond
                val expiredTime = issuedTime + tokenExpiration
                val groups: Set<String>? = user.roles?.map { it.value }?.toSet()
                Authentication(
                    username = user.username,
                    token = tokenUtils.generateToken(user.username, groups, tokenExpiration, tokenIssuer),
                    expiration = expiredTime
                )
            } else {
                null
            }
        }

    fun findAll(): List<User> =
        userRepository.findAll().list()

    fun findById(id: Long): User? =
        userRepository.findById(id)

    @Transactional
    fun create(user: User): User {
        user.password = BcryptUtil.bcryptHash(user.password)
        userRepository.persist(user)
        return user
    }

    @Transactional
    fun update(id: Long, user: User): User? {
        return userRepository.findById(id)?.let { persistedUser ->
            persistedUser.username = user.username
            persistedUser.password = user.password
            persistedUser.roles = user.roles
            persistedUser
        }
    }

    @Transactional
    fun delete(id: Long): Boolean =
        userRepository.deleteById(id)

}