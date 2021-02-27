package net.thenova.titan.module.sqldatabase.sql;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.zaxxer.hikari.HikariDataSource;
import de.arraying.kotys.JSON;
import de.arraying.lumberjack.LFsRules;
import de.arraying.lumberjack.LLogLevel;
import de.arraying.lumberjack.LLogger;
import de.arraying.lumberjack.LLoggerBuilder;
import lombok.Getter;
import net.thenova.titan.Titan;
import net.thenova.titan.json.JSONFile;
import net.thenova.titan.json.JSONFileData;
import net.thenova.titan.module.ModuleManager;
import net.thenova.titan.module.sqldatabase.settings.DatabaseConnectionSettings;
import net.thenova.titan.module.sqldatabase.tables.Database;
import net.thenova.titan.module.sqldatabase.tables.DatabaseTable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Copyright 2020 ipr0james
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
@Getter
public enum SQLConnectionHandler {
    INSTANCE;

    private JSONFile file;
    private LLogger logger;

    private ListeningExecutorService executorService;
    private boolean debugToConsole;

    private final Map<String, HikariDataSource> sources = new HashMap<>();

    private long executions;
    private long statements;

    public void init() {
        this.file = JSONFile.create(new JSONFileData() {
            @Override
            public final String name() {
                return "database";
            }

            @Override
            public final String path() {
                return ModuleManager.INSTANCE.getDirectoryData() + File.separator;
            }

            @Override
            public final ClassLoader loader() {
                return this.getClass().getClassLoader();
            }
        });
        final JSON config = this.file.getJson().json("config");

        this.logger = LLoggerBuilder.create("sqldatabase")
                .withRouteFs(LLogLevel.INFO, new LFsRules() {
                    @Override
                    public File getDirectory() {
                        return new File(Titan.INSTANCE.getDataRoot(), "logs");
                    }

                    @Override
                    public int getLineLimit() {
                        return -1;
                    }

                    @Override
                    public long getTimeLimit() {
                        return TimeUnit.DAYS.toMillis(1);
                    }

                    @Override
                    public String formatFileName(final long time, final long uid) {
                        return String.format("sqldatabse-%d.txt", uid);
                    }
                })
                .withThreadPoolSize(1)
                .build();

        this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(config.integer("thread-pool-size")));
        this.debugToConsole = config.bool("debug-to-console");

        final JSON databases = this.file.getJson().json("databases");
        databases.raw()
                .keySet()
                .forEach(this::loadSource);
    }

    public void shutdown() {
        this.sources.values().forEach(HikariDataSource::close);
        this.sources.clear();
    }

    /**
     * Load a HikariDataSource into map
     *
     * @param key - String
     */
    private void loadSource(final String key) {
        final DatabaseConnectionSettings settings = this.file.getJson()
                .json("databases")
                .json(key)
                .marshal(DatabaseConnectionSettings.class);

        try {
            this.sources.put(key, settings.build(key));
        } catch (final SQLDatabaseException ignored) { }
    }

    /**
     * Return HikariDataSource, attempt loading if not found.
     *
     * @param database - Database
     * @return - HikariDataSource
     */
    public final HikariDataSource getSource(final Database database) {
        final String name = database.name();

        if(!this.sources.containsKey(name)) {
            final JSON json = this.file.getJson();
            if(json.json("databases").json(name) == null) {
                json.json("databases").put(name, new DatabaseConnectionSettings());
                this.file.save(json);

                Titan.INSTANCE.getLogger().info("[SQLConnectionHandler] - Configuration is required for '%s' in database.json", name);
            } else {
                this.loadSource(name);
            }
        }

        return this.sources.get(name);
    }

    /**
     * Create a List of tables
     *
     * @param tables - List<DatabaseTable>
     */
    @SuppressWarnings("UnstableApiUsage")
    public final ListenableFuture<Void> createTables(final List<DatabaseTable> tables) {
        final Map<String, SQLExecutor> executors = new HashMap<>();
        tables.forEach(table -> {
            if(executors.containsKey(table.getDatabase().name())) {
                executors.get(table.getDatabase().name()).add(table.build());
            } else {
                executors.put(table.getDatabase().name(), table.build()); }
        });

        return Futures.transform(Futures.allAsList(executors.values()
                        .stream()
                        .map(SQLExecutor::transaction)
                        .collect(Collectors.toList())),
                new Function<List<Void>, Void>() {
                    @Override
                    public final @Nullable Void apply(final @Nullable List<Void> voids) {
                        return null;
                    }
                }, this.executorService);
    }

    public void incrementExecutions() {
        this.executions++;
    }

    public void incrementStatements(final long amount) {
        this.statements += amount;
    }
}
