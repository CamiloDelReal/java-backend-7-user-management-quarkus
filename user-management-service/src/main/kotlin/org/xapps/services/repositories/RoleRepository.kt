package org.xapps.services.repositories

import io.quarkus.hibernate.orm.panache.PanacheRepository
import org.xapps.services.entities.Role
import java.util.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class RoleRepository: PanacheRepository<Role> {

    fun findByValue(value: String): Optional<Role> =
        find(Role.VALUE, value).firstResultOptional()

}