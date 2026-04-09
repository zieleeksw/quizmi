package pl.zieleeksw.quizmi

import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Import(IntegrationTest.TestContainersConfiguration::class)
abstract class IntegrationTest {

    @Autowired(required = false)
    var jdbcTemplate: JdbcTemplate? = null

    @TestConfiguration(proxyBeanMethods = false)
    class TestContainersConfiguration {

        @Bean
        @ServiceConnection
        fun postgresContainer(): PostgreSQLContainer {
            return PostgreSQLContainer("postgres:15").apply {
                withDatabaseName("quizmi_test")
                withUsername("quizmi")
                withPassword("quizmi")
            }
        }
    }

    @AfterEach
    fun cleanDatabase() {
        val template = jdbcTemplate ?: return
        if (!tableExists(template, "users")) {
            return
        }

        template.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE")
    }

    private fun tableExists(
        jdbcTemplate: JdbcTemplate,
        tableName: String
    ): Boolean {
        val sql = """
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = ?
            )
        """.trimIndent()

        return jdbcTemplate.queryForObject(sql, Boolean::class.java, tableName) ?: false
    }
}
