package org.xapps.services.entities

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.security.jpa.Password
import io.quarkus.security.jpa.Roles
import io.quarkus.security.jpa.UserDefinition
import io.quarkus.security.jpa.Username
import javax.persistence.*

@Entity
@Table(name = User.TABLE_NAME)
@NamedQueries(
    NamedQuery(name = "User.findByIdNotAndUsername", query = "from User where id != ?1 and username = ?2")
)
@UserDefinition
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = ID)
    @JsonProperty(value = ID)
    var id: Long = 0,

    @Column(name = USERNAME)
    @JsonProperty(value = USERNAME)
    @Username
    var username: String? = "",

    @Column(name = PASSWORD)
    @JsonProperty(value = PASSWORD, access = JsonProperty.Access.WRITE_ONLY)
    @Password
    var password: String? = "",

    @ManyToMany
    @JsonProperty(value = ROLES)
    @Roles
    var roles: List<Role>? = null
) {
    companion object {
        const val TABLE_NAME = "users"
        const val ID = "id"
        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val ROLES = "roles"
    }

}