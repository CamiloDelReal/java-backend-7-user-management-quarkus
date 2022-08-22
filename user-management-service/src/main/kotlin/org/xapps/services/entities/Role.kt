package org.xapps.services.entities

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.security.jpa.RolesValue
import javax.persistence.*

@Entity
@Table(name = Role.TABLE_NAME)
@NamedQueries(
    NamedQuery(name = "Role.findByIds", query = "from Role where id in ?1")
)
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = ID)
    @JsonProperty(value = ID)
    var id: Long = 0,

    @Column(name = VALUE)
    @JsonProperty(value = VALUE)
    @RolesValue
    var value: String? = ""
) {
    companion object {
        const val TABLE_NAME = "roles"
        const val ID = "id"
        const val VALUE = "value"

        const val ADMINISTRATOR = "administrator"
        const val GUEST = "guest"
    }
}