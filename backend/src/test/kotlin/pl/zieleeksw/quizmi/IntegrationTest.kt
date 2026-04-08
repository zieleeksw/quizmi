package pl.zieleeksw.quizmi

import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("integration")
abstract class IntegrationTest {

    @Autowired(required = false)
    var jdbcTemplate: JdbcTemplate? = null

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15").apply {
            withDatabaseName("quizmi_test")
            withUsername("quizmi")
            withPassword("quizmi")
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }

            registry.add("spring.liquibase.url", postgres::getJdbcUrl)
            registry.add("spring.liquibase.user", postgres::getUsername)
            registry.add("spring.liquibase.password", postgres::getPassword)
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
