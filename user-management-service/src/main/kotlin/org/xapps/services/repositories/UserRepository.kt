package org.xapps.services.repositories

import io.quarkus.hibernate.orm.panache.PanacheRepository
import org.xapps.services.entities.User
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserRepository : PanacheRepository<User> {

    fun findByUsername(username: String): User? =
        find(User.USERNAME, username).firstResult()

}