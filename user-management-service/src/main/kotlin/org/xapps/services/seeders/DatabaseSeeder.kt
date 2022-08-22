package org.xapps.services.seeders

import io.quarkus.elytron.security.common.BcryptUtil
import io.quarkus.runtime.StartupEvent
import org.xapps.services.entities.Role
import org.xapps.services.entities.User
import org.xapps.services.repositories.RoleRepository
import org.xapps.services.repositories.UserRepository
import javax.enterprise.event.Observes
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@Singleton
class DatabaseSeeder @Inject constructor(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun seedDatabase(@Observes event: StartupEvent) {
        var adminRole: Role? = null
        if (roleRepository.count() == 0L) {
            adminRole = Role(value = Role.ADMINISTRATOR)
            val guestRole = Role(value = Role.GUEST)
            roleRepository.persist(adminRole, guestRole)
        }

        if (userRepository.count() == 0L) {
            if (adminRole == null) {
                adminRole = roleRepository.findByValue(Role.ADMINISTRATOR)!!
            }
            val admin = User(
                username = "root",
                password = BcryptUtil.bcryptHash("123456"),
                roles = mutableListOf(adminRole)
            )
            userRepository.persist(admin)
        }
    }

}