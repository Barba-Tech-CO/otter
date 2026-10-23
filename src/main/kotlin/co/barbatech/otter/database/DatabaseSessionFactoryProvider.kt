package co.barbatech.otter.database

import co.barbatech.otter.database.session.DatabaseSessionFactory
import co.barbatech.otter.database.session.JdbcDatabaseSessionFactory

/** Single place the app asks for the JDBC session factory. */
object DatabaseSessionFactoryProvider {
  fun jdbcSessionFactory(): DatabaseSessionFactory = JdbcDatabaseSessionFactory()
}
