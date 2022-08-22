package org.xapps.services

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class UserResourceTest {

    @Test
    fun testGetAllUers() {
        given()
          .`when`().get("/users")
          .then()
             .statusCode(200)
    }

}