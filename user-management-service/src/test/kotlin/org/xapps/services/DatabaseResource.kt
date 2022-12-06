package org.xapps.services

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.MySQLContainer


class DatabaseResource : QuarkusTestResourceLifecycleManager {

    override fun start(): Map<String, String> {
        mysqlContainer.start()
        return mapOf(
            "quarkus.datasource.jdbc.url" to mysqlContainer.jdbcUrl,
            "quarkus.datasource.username" to mysqlContainer.username,
            "quarkus.datasource.password" to mysqlContainer.password
        )
    }

    override fun stop() {
        mysqlContainer.stop()
    }

    companion object {
        val mysqlContainer: MySQLContainer<*> = MySQLContainer("mysql:8.0")
                .withDatabaseName("users_management_test")
                .withUsername("testroot")
                .withPassword("testroot")
    }
}