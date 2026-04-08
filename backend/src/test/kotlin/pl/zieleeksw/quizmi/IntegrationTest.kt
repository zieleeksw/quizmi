package pl.zieleeksw.quizmi

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer


@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
abstract class IntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15").apply {
            withDatabaseName("quizmi_test")
            withUsername("quizmi")
            withPassword("quizmi")
        }
    }
}
