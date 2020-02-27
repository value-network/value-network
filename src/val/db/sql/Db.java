package val.db.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.JDBCUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.ValNetwork;
import val.db.cache.DBCacheManagerImpl;
import val.db.store.Dbs;
import val.props.PropertyService;
import val.props.Props;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public final class Db {

    private static final Logger logger = LoggerFactory.getLogger(Db.class);
    private static final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Map<DbKey, Object>>> transactionCaches = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Map<DbKey, Object>>> transactionBatches = new ThreadLocal<>();
    private static HikariDataSource cp;
    private static SQLDialect dialect;
    private static DBCacheManagerImpl dbCacheManager;

    private Db() {
    } // never

    public static void init(PropertyService propertyService, DBCacheManagerImpl dbCacheManager) {
        Db.dbCacheManager = dbCacheManager;

        String dbUrl;
        String dbUsername;
        String dbPassword;

        logger.info("Test NET: {}", ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET));

        if (ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET)) {
            dbUrl = propertyService.getString(Props.DEV_DB_URL);
            dbUsername = propertyService.getString(Props.DEV_DB_USERNAME);
            dbPassword = propertyService.getString(Props.DEV_DB_PASSWORD);
        } else {
            dbUrl = propertyService.getString(Props.DB_URL);
            dbUsername = propertyService.getString(Props.DB_USERNAME);
            dbPassword = propertyService.getString(Props.DB_PASSWORD);
        }
        dialect = JDBCUtils.dialect(dbUrl);

        logger.info("DB USER: {}, PASSWORD: {}, dbUrl: {}", dbUsername, dbPassword, dbUrl);

        logger.debug("Database jdbc url set to: " + dbUrl);
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            if (dbUsername != null)
                config.setUsername(dbUsername);
            if (dbPassword != null)
                config.setPassword(dbPassword);

            config.setMaximumPoolSize(propertyService.getInt(Props.DB_CONNECTIONS));

            FluentConfiguration flywayBuilder = Flyway.configure()
                    .dataSource(dbUrl, dbUsername, dbPassword)
                    .baselineOnMigrate(true);
            boolean runFlyway = false;

            switch (dialect) {
                case MYSQL:
                case MARIADB:
                    flywayBuilder.locations("classpath:/db/mysql");
                    runFlyway = true;
                    config.setAutoCommit(true);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("characterEncoding", "utf8mb4");
                    config.addDataSourceProperty("useUnicode", "true");
                    config.addDataSourceProperty("useServerPrepStmts", "false");
                    config.addDataSourceProperty("rewriteBatchedStatements", "true");
                    config.setConnectionInitSql("SET NAMES utf8mb4;");
                    break;
                case H2:
                    Class.forName("org.h2.Driver");
                    flywayBuilder.locations("classpath:/db/migration_h2");
                    runFlyway = true;
                    config.setAutoCommit(true);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("DATABASE_TO_UPPER", "false");
                    break;
            }
            // config.setLeakDetectionThreshold(2000);


            logger.info("*** DB USER: {}, PASSWORD: {}, dbUrl: {}", config.getUsername(), config.getPassword(), config.getJdbcUrl());
            cp = new HikariDataSource(config);

            if (dialect == SQLDialect.H2) {
                int defaultLockTimeout = propertyService.getInt(Props.DB_LOCK_TIMEOUT) * 1000;
                try (Connection con = cp.getConnection();
                     PreparedStatement stmt = con.prepareStatement("SET DEFAULT_LOCK_TIMEOUT ?")) {
                    // stmt.executeUpdate(defaultLockTimeout);
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }

            if (runFlyway) {
                logger.info("Running flyway migration");
                Flyway flyway = flywayBuilder.load();
                flyway.repair();
                flyway.migrate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Dbs getDbsByDatabaseType() {
        logger.info("Using SQL Backend with Dialect {}", dialect.getName());
        return new SqlDbs();
    }


    public static void analyzeTables() {
        if (dialect == SQLDialect.H2) {
            try (Connection con = cp.getConnection();
                 Statement stmt = con.createStatement()) {
                stmt.execute("ANALYZE SAMPLE_SIZE 0");
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

    public static void shutdown() {
        if (dialect == SQLDialect.H2) {
            try (Connection con = cp.getConnection(); Statement stmt = con.createStatement()) {
                // COMPACT is not giving good result.
                if (ValNetwork.getPropertyService().getBoolean(Props.DB_H2_DEFRAG_ON_SHUTDOWN)) {
                    stmt.execute("SHUTDOWN DEFRAG");
                } else {
                    stmt.execute("SHUTDOWN");
                }
            } catch (SQLException e) {
                logger.info(e.toString(), e);
            } finally {
                logger.info("Database shutdown completed.");
            }
        }
        if (!cp.isClosed()) {
            cp.close();
        }
    }

    private static Connection getPooledConnection() throws SQLException {
        return cp.getConnection();
    }

    public static Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null) {
            return con;
        }
        con = getPooledConnection();
        con.setAutoCommit(true);

        return new DbConnection(con);
    }

    public static DSLContext getDSLContext() {
        Connection con = localConnection.get();
        Settings settings = new Settings();
        settings.setRenderSchema(Boolean.FALSE);

        if (con == null) {
            try (DSLContext ctx = DSL.using(cp, dialect, settings)) {
                return ctx;
            }
        } else {
            try (DSLContext ctx = DSL.using(con, dialect, settings)) {
                return ctx;
            }
        }
    }

    static Map<DbKey, Object> getCache(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        return transactionCaches.get().computeIfAbsent(tableName, k -> new HashMap<>());
    }

    static Map<DbKey, Object> getBatch(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        return transactionBatches.get().computeIfAbsent(tableName, k -> new HashMap<>());
    }

    public static boolean isInTransaction() {
        return localConnection.get() != null;
    }

    public static Connection beginTransaction() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = cp.getConnection();
            con.setAutoCommit(false);

            con = new DbConnection(con);

            localConnection.set((DbConnection) con);
            transactionCaches.set(new HashMap<>());
            transactionBatches.set(new HashMap<>());

            return con;
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void commitTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void rollbackTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doRollback();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        transactionCaches.get().clear();
        transactionBatches.get().clear();
        dbCacheManager.flushCache();
    }

    public static void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        transactionCaches.get().clear();
        transactionCaches.set(null);
        transactionBatches.get().clear();
        transactionBatches.set(null);
        DbUtils.close(con);
    }

    private static class DbConnection extends FilteredConnection {

        private DbConnection(Connection con) {
            super(con);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void commit() throws SQLException {
            if (localConnection.get() == null) {
                super.commit();
            } else if (!this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                throw new UnsupportedOperationException("Use Db.commitTransaction() to commit the transaction");
            }
        }

        private void doCommit() throws SQLException {
            super.commit();
        }

        @Override
        public void rollback() throws SQLException {
            if (localConnection.get() == null) {
                super.rollback();
            } else if (!this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                throw new UnsupportedOperationException("Use Db.rollbackTransaction() to rollback the transaction");
            }
        }

        private void doRollback() throws SQLException {
            super.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (localConnection.get() == null) {
                super.close();
            } else if (!this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }

    }
}
