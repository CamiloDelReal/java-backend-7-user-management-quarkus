package org.xapps.services.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.security.jpa.RolesValue
import javax.persistence.*

@Entity
@Table(name = Role.TABLE_NAME)
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = ID)
    @JsonProperty(value = ID)
    var id: Long = 0,

    @Column(name = VALUE)
    @JsonProperty(value = VALUE)
    @RolesValue
    var value: String = "",

    @ManyToMany(mappedBy = User.ROLES)
    @JsonIgnore
    var users: List<User>? = null
) {
    companion object {
        const val TABLE_NAME = "roles"
        const val ID = "id"
        const val VALUE = "value"

        const val ADMINISTRATOR = "administrator"
        const val GUEST = "guest"
    }
}