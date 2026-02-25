package studio.tearule.db

import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import studio.tearule.db.tables.Rules
import studio.tearule.db.tables.TeaLots
import java.sql.Connection

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        // Use environment variables if available, otherwise fallback to config
        val url = System.getenv("DATABASE_URL") ?: config.property("database.url").getString()
        val driver = System.getenv("DATABASE_DRIVER") ?: config.property("database.driver").getString()
        val user = System.getenv("DATABASE_USER") ?: config.property("database.user").getString()
        val password = System.getenv("DATABASE_PASSWORD") ?: config.property("database.password").getString()

        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password,
        )

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED

        transaction {
            SchemaUtils.create(TeaLots, Rules)
        }
    }
}
