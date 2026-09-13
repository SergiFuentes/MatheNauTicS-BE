package com.mathenautics.backend.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    companion object {

        @JvmStatic
        @ServiceConnection
        val postgresContainer = PostgreSQLContainer("postgres:16.4").apply {
            withDatabaseName("mathenautics_test")
            withUsername("test_user")
            withPassword("test_password")
        }

        init {
            postgresContainer.start()
        }
    }
}