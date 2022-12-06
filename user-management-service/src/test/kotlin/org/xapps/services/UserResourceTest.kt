package org.xapps.services

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.xapps.services.entities.Role
import java.util.*

@QuarkusTest
@QuarkusTestResource(DatabaseResource::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserResourceTest {

    companion object {
        private var adminToken: String? = null
        private var userCreatedWithDefaultRole: ExtractableResponse<*>? = null
        private var userCreatedWithDefaultRolePassword: String? = null
    }

    @Test
    @Order(1)
    fun loginRoot_success() {
        val result = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "root",
                    "password" : "123456"
                  }
                  """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        assertNotNull(result.path<String>("token"))

        adminToken = result.path<String>("token")
    }

    @Test
    @Order(2)
    fun loginRoot_failByInvalidPassword() {
        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "root",
                    "password" : "invalid"
                  }
                  """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
    }

    @Test
    @Order(3)
    fun login_failByInvalidCredentials() {
        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "invalid",
                    "password" : "123456"
                  }
                  """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
    }

    @Test
    @Order(4)
    fun createUserWithDefaultRole_success() {
        val testPassword = "qwerty"

        val result = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "johndoe",
                    "password" : "$testPassword"
                  }
                  """.trimIndent())
                .post("/users")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        assertNotNull(result.path<String>("id"))
        assertNotEquals(0, result.path<String>("id"))
        assertEquals("johndoe", result.path<String>("username"))
        assertNotNull(result.path<String>("username"))
        val roles: List<String> = result.jsonPath().getList("roles.value")
        assertEquals(1, roles.size)
        assertEquals(Role.GUEST, roles[0])

        userCreatedWithDefaultRole = result
        userCreatedWithDefaultRolePassword = testPassword
    }

    @Test
    @Order(5)
    fun createUserWithAdminRole_failByNoAdminCredentials() {
        val rolesResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/users/roles")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        val rolesIds: List<Long> = rolesResult.jsonPath().getList("id")
        val rolesNames: List<String> = rolesResult.jsonPath().getList("value")
        assertEquals(2, rolesIds.size)
        assertEquals(2, rolesNames.size)

        val indexAdminRole = rolesNames.indexOf(Role.ADMINISTRATOR)

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "johndoe",
                    "password" : "qwerty",
                    "roles": [
                      {
                        "id": ${rolesIds[indexAdminRole]},
                        "value": "${rolesNames[indexAdminRole]}"
                      }
                    ]
                  }
                  """.trimIndent())
                .post("/users")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
    }

    @Test
    @Order(6)
    fun createUserWithAdminRole_success() {
        assertNotNull(adminToken)

        val rolesResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/users/roles")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        val rolesIds: List<Long> = rolesResult.jsonPath().getList("id")
        val rolesNames: List<String> = rolesResult.jsonPath().getList("value")
        assertEquals(2, rolesIds.size)
        assertEquals(2, rolesNames.size)

        val indexAdminRole = rolesNames.indexOf(Role.ADMINISTRATOR)

        val result = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .body("""
                  {
                    "username" : "kathdoe",
                    "password" : "qwerty",
                    "roles": [
                      {
                        "id": ${rolesIds[indexAdminRole]},
                        "value": "${rolesNames[indexAdminRole]}"
                      }
                    ]
                  }
                  """.trimIndent())
                .post("/users")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        assertNotNull(result.path<Long>("id"))
        assertNotEquals(0, result.path<Long>("id"))
        assertEquals("kathdoe", result.path<String>("username"))
        assertNull(result.path<String>("password"))
        val roles: List<String> = result.jsonPath().getList("roles.value")
        assertEquals(1, roles.size)
        assertEquals(Role.ADMINISTRATOR, roles[0])
    }

    @Test
    @Order(7)
    fun createUser_failByUsernameDuplicity() {
        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "root",
                    "password" : "qwerty"
                  }
                  """.trimIndent())
                .post("/users")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
    }

    @Test
    @Order(8)
    fun editUserWithUserCredentials_success() {
        assertNotNull(userCreatedWithDefaultRole)
        assertNotNull(userCreatedWithDefaultRolePassword)

        val testPassword = "12345"
        val loginEditResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "${userCreatedWithDefaultRole!!.path<String>("username")}",
                    "password" : "$userCreatedWithDefaultRolePassword"
                  }
                  """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        val token = loginEditResult.path<String>("token")
        assertNotNull(token)

        val userEditResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body("""
                  {
                    "username" : "annadoe",
                    "password" : "$testPassword"
                  }
                  """.trimIndent())
                .put("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id"))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        assertEquals(userCreatedWithDefaultRole!!.path<Long>("id"), userEditResult.path<Long>("id"))
        assertEquals("annadoe", userEditResult.path<String>("username"))
        val roles = userEditResult.jsonPath().getList<String>("roles.value")
        assertEquals(1, roles.size)
        assertEquals(Role.GUEST, roles[0])

        userCreatedWithDefaultRole = userEditResult
        userCreatedWithDefaultRolePassword = testPassword

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "annadoe",
                    "password" : "$testPassword"
                  }
                  """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
    }

    @Test
    @Order(9)
    fun editUserWithUserCredentials_failByWrongId() {
        assertNotNull(userCreatedWithDefaultRole)
        assertNotNull(userCreatedWithDefaultRolePassword)

        val loginResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                      "username": "${userCreatedWithDefaultRole!!.path<String>("username")}",
                      "password": "$userCreatedWithDefaultRolePassword"
                    }
                """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResult.path<String>("token")}")
                .body("""
                    {
                      "username": "newusername",
                      "password": "newpassword"
                    }
                """.trimIndent())
                .put("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id") + 999)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
    }

    @Test
    @Order(10)
    fun editUserToAdminWithUserCredentials_failByNoAdminCredentials() {
        assertNotNull(userCreatedWithDefaultRole)
        assertNotNull(userCreatedWithDefaultRolePassword)

        val loginEditResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                      "username": "${userCreatedWithDefaultRole!!.path<String>("username")}",
                      "password": "$userCreatedWithDefaultRolePassword"
                    }
                """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        val rolesResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/users/roles")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        val rolesIds: List<Long> = rolesResult.jsonPath().getList("id")
        val rolesNames: List<String> = rolesResult.jsonPath().getList("value")
        assertEquals(2, rolesIds.size)
        assertEquals(2, rolesNames.size)

        val indexAdminRole = rolesNames.indexOf(Role.ADMINISTRATOR)

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginEditResult.path<String>("token")}")
                .body("""
                  {
                    "username" : "bethdoe",
                    "password" : "poiuy",
                    "roles": [
                      {
                        "id": ${rolesIds[indexAdminRole]},
                        "value": "${rolesNames[indexAdminRole]}"
                      }
                    ]
                  }
                  """.trimIndent())
                .put("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id"))
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
    }

    @Test
    @Order(11)
    fun editUser_failByNoUserCredentials() {
        assertNotNull(userCreatedWithDefaultRole)

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                  {
                    "username" : "annadoe",
                    "password" : "12345"
                  }
                  """.trimIndent())
                .put("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id"))
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
    }

    @Test
    @Order(12)
    fun editUserWithAdminCredentials_success() {
        assertNotNull(userCreatedWithDefaultRole)
        assertNotNull(adminToken)

        val testPassword = "zxcvb"
        val userEditResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .body("""
                    {
                      "username": "sarahdoe",
                      "password": "$testPassword"
                    }
                """.trimIndent())
                .put("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id"))
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        assertEquals(userCreatedWithDefaultRole!!.path<Long>("id"), userEditResult.path<Long>("id"))
        assertEquals("sarahdoe", userEditResult.path<String>("username"))

        val oldRoles = userCreatedWithDefaultRole!!.jsonPath().getList<String>("roles.value")
        val newRoles = userEditResult.jsonPath().getList<String>("roles.value")
        assertEquals(oldRoles.size, newRoles.size)
        assertEquals(oldRoles[0], newRoles[0])

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                      "username": "sarahdoe",
                      "password": "$testPassword"
                    }
                """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)

        userCreatedWithDefaultRole = userEditResult
        userCreatedWithDefaultRolePassword = testPassword
    }

    @Test
    @Order(13)
    fun deleteUser_failByNoCredentials() {
        assertNotNull(userCreatedWithDefaultRole)

        given()
                .`when`()
                .delete("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id"))
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
    }

    @Test
    @Order(14)
    fun deleteUserWithUserCredentials_success() {
        assertNotNull(userCreatedWithDefaultRole)
        assertNotNull(userCreatedWithDefaultRolePassword)

        val loginDeleteResult = given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                      "username": "${userCreatedWithDefaultRole!!.path<String>("username")}",
                      "password": "$userCreatedWithDefaultRolePassword"
                    }
                """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()

        given()
                .`when`()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginDeleteResult.path<String>("token")}")
                .delete("/users/{id}", userCreatedWithDefaultRole!!.path<Long>("id"))
                .then()
                .statusCode(HttpStatus.SC_OK)

        given()
                .`when`()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                      "username": "${userCreatedWithDefaultRole!!.path<String>("username")}",
                      "password": "$userCreatedWithDefaultRolePassword"
                    }
                """.trimIndent())
                .post("/users/login")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)

        userCreatedWithDefaultRole = null
        userCreatedWithDefaultRolePassword = null
    }
}