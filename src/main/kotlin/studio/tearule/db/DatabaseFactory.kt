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
        val url = config.property("database.url").getString()
        val driver = config.property("database.driver").getString()
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()

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
