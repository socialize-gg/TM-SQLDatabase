package net.thenova.titan.module.sqldatabase.settings;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import de.arraying.kotys.JSONField;
import lombok.Getter;
import net.thenova.titan.Titan;
import net.thenova.titan.module.sqldatabase.sql.SQLDatabaseException;

import java.sql.SQLException;

/**
 * Copyright 2019 ipr0james
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
public final class DatabaseConnectionSettings {

    @JSONField(key = "host") private String host = "localhost";
    @JSONField(key = "port") private int port = 3306;
    @JSONField(key = "database") private String database = "database";
    @JSONField(key = "user") private String user = "user";
    @JSONField(key = "password") private String password = "password";
    @JSONField(key = "max-connections") private int maxConnections = 5;

    public final HikariDataSource build(final String key) throws SQLDatabaseException {
        final HikariDataSource source = new HikariDataSource();

        source.setDriverClassName("org.mariadb.jdbc.Driver");
        source.setJdbcUrl("jdbc:mariadb://" + this.host + ":" + this.port + "/" + this.database);
        source.setPoolName("titan-" + key);
        source.setUsername(this.user);
        source.setPassword(this.password);

        source.setIdleTimeout(20000);
        source.setMaxLifetime(60000);
        source.setMinimumIdle(1);
        source.setMaximumPoolSize(this.maxConnections);

        source.setLeakDetectionThreshold(2000L);
        source.addDataSourceProperty("useSSL", "false");
        source.addDataSourceProperty("useUnicode", "true");
        source.addDataSourceProperty("characterEncoding", "utf-8");
        source.addDataSourceProperty("autoReconnect", "true");

        source.addDataSourceProperty("cachePrepStmts", "true");
        source.addDataSourceProperty("prepStmtCacheSize", "250");
        source.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        source.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            source.getConnection().close();
            Titan.INSTANCE.getLogger().debug("[DatabaseConnectionSettings] - Connection has successfully been established for '%s'.", key);
        } catch (final HikariPool.PoolInitializationException | SQLException ex) {
            throw new SQLDatabaseException("Connection failed to establish for '" + key + "'", ex);
        }

        return source;
    }
}
