package pl.zieleeksw.quizmi

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostgresContainerIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun `should connect to postgres testcontainer`() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select current_database()").use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals("quizmi_test", resultSet.getString(1))
                }
            }
        }
    }
}
