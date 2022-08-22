package org.xapps.services.repositories

import io.quarkus.hibernate.orm.panache.PanacheRepository
import org.xapps.services.entities.Role
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class RoleRepository : PanacheRepository<Role> {

    fun findByValue(value: String): Role? =
        find(Role.VALUE, value).firstResult()

    fun findByIds(ids: List<Long>): List<Role> =
        find("#Role.findByIds", ids).list()

}