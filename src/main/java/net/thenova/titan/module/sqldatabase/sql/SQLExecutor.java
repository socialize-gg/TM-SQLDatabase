package net.thenova.titan.module.sqldatabase.sql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.zaxxer.hikari.HikariDataSource;
import de.arraying.lumberjack.LLogger;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.thenova.titan.Titan;
import net.thenova.titan.module.sqldatabase.tables.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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
public final class SQLExecutor {
    private enum Type {
        UPDATE,
        SELECT
    }

    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class SQLOperation {
        private final Type type;
        private final String query;
        private final Object[] parameters;

        private Consumer<ResultSet> result;
    }

    private final HikariDataSource source;
    private final LLogger logger;
    private final List<SQLOperation> operations = new ArrayList<>();

    private Connection connection;

    public SQLExecutor(final Database database) {
        this.source = SQLConnectionHandler.INSTANCE.getSource(database);
        this.logger = SQLConnectionHandler.INSTANCE.getLogger();
    }

    /**
     * Combine 2 SQLExecutors in to 1 object
     *
     * @param executors SQLExecutor to be combined
     * @return current object
     */
    public final SQLExecutor add(final SQLExecutor... executors) {
        Arrays.stream(executors)
                .forEach(executor -> this.operations.addAll(executor.operations));

        return this;
    }

    /**
     * Query for adding an update to the transaction
     *
     * @param query String
     * @param parameters Object
     * @return SQLExecutor
     */
    public final SQLExecutor queryUpdate(final String query, final Object... parameters) {
        this.operations.add(new SQLOperation(Type.UPDATE, query, parameters));

        return this;
    }

    /**
     * Query for retrieval of data
     *
     * @param query String
     * @param parameters Object
     * @return - SQLExecutor
     */
    public final SQLExecutor querySelect(final String query, final Object... parameters) {
        final SQLOperation operation = new SQLOperation(Type.SELECT, query, parameters);
        this.operations.add(operation);

        return this;
    }

    /**
     * Handle results for querySelects
     *
     * @param result ResultSet
     * @return SQLExecutor
     */
    public final SQLExecutor result(final Consumer<ResultSet> result) {
        try {
            if (this.operations.isEmpty()) {
                throw new SQLDatabaseException("Tried to retrieve results when no queries were present.");
            }

            final SQLOperation operation;
            try {
                operation = this.operations.get(this.operations.size()-1);
            } catch (final ArrayIndexOutOfBoundsException ex) {
                throw new SQLDatabaseException("Failed to retrieve last result of Operations List");
            }

            if(operation.type != Type.SELECT) {
                throw new SQLDatabaseException("Last operation was not a SELECT");
            }

            operation.result = result;
        } catch (final SQLDatabaseException ignored) {}

        return this;
    }

    /**
     *
     *
     * @return
     */
    public final ListenableFuture<Void> commit() {
        if(this.source == null) {
            Titan.INSTANCE.getLogger().debug("[SQLDatabase] [SQLExecutor] - Avoided commit due to connection source null");
            return Futures.immediateFuture(null);
        }

        final long start = System.currentTimeMillis();
        return SQLConnectionHandler.INSTANCE.getExecutorService().submit(() -> {
            final long time = System.currentTimeMillis();
            this.handle();
            this.close();

            this.logger.info("[SQLExecutor] [handleCommit] - Commit completion, internal: %d, full: %d",
                    System.currentTimeMillis() - start,
                    System.currentTimeMillis() - time);
            return null;
        });
    }

    public final ListenableFuture<Void> transaction() {
        if(this.source == null) {
            Titan.INSTANCE.getLogger().debug("[SQLDatabase] [SQLExecutor] - Avoided commit due to connection source null");
            return Futures.immediateVoidFuture();
        }

        final long start = System.currentTimeMillis();
        return SQLConnectionHandler.INSTANCE.getExecutorService().submit(() -> {
            final long time = System.currentTimeMillis();
            final Connection connection = this.connection();

            connection.setAutoCommit(false);
            this.handle();
            connection.commit();
            this.close();

            this.logger.info("[SQLExecutor] [handleTransaction] - Transaction completion, internal: %d, full: %d",
                    System.currentTimeMillis() - start,
                    System.currentTimeMillis() - time);

            return null;
        });
    }

    /**
     * Handle all Operations for executor when called
     */
    private void handle() {
        SQLConnectionHandler.INSTANCE.incrementExecutions();
        SQLConnectionHandler.INSTANCE.incrementStatements(this.operations.size());

        this.operations.forEach(operation -> {
            final long time = System.currentTimeMillis();
            try {
                try {
                    final PreparedStatement statement = SQLExecutor.this.statement(operation.query, operation.parameters);
                    if (operation.type == Type.SELECT) {
                        final ResultSet result = statement.executeQuery();

                        if(operation.result != null) {
                            operation.result.accept(result);
                        } else {
                            throw new SQLDatabaseException("Failed to return ResultSet as operation.result was null");
                        }
                    } else {
                        statement.execute();
                    }
                } catch (final SQLException ex) {
                    throw new SQLDatabaseException("Failed to execute query '"
                            + operation.query
                            + "' with ["
                            + Arrays.stream(operation.parameters)
                                .map(Object::toString)
                                .collect(Collectors.joining(", "))
                            + "]", ex);
                }
            } catch (final SQLDatabaseException ignored) {}

            this.logger.info("[SQLExecutor] [handle] - Completion time %d, Parameters [%s], Statement: '%s'",
                    System.currentTimeMillis() - time,
                    Arrays.stream(operation.parameters)
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")),
                    operation.query);
        });
    }

    /**
     * Build PreparedStatement
     *
     * @param query Query to be built
     * @param parameters Parameters as object to be parsed
     * @return PreparedStatement
     * @throws SQLDatabaseException Custom event for logging
     */
    private PreparedStatement statement(final String query, final Object... parameters) throws SQLDatabaseException {
        try {
            final PreparedStatement statement = this.connection()
                    .prepareStatement(query);
            for (int i = 0; i < parameters.length; i++) {
                final Object param = parameters[i];
                if(param == null) {
                    statement.setNull(i + 1, Types.JAVA_OBJECT);
                } else {
                    statement.setObject(i + 1, param);
                }
            }

            return statement;
        } catch (final SQLException ex) {
            throw new SQLDatabaseException("Failed when building PreparedStatement for query '" + query +
                    "' with [" + Arrays.stream(parameters).map(Object::toString).collect(Collectors.joining(", ")) + "]" );
        }
    }


    /**
     * Establish the Java SQL connection
     *
     * @return Build a connection for the Database selected
     * @throws SQLDatabaseException Thrown for either failed connection establishment or connection closed on use.
     */
    private Connection connection() throws SQLDatabaseException {
        if(this.connection == null) {
            try {
                this.connection = this.source.getConnection();
            } catch (final SQLException ex) {
                throw new SQLDatabaseException("Connection could not be established for source pool: " + this.source.getPoolName(), ex);
            }
        }

        try {
            if(this.connection.isClosed()) {
                throw new SQLDatabaseException("Connection was closed when trying to fetch");
            }
        } catch (final SQLException ex) {
            throw new SQLDatabaseException("Failed to check if connection was closed", ex);
        }

        return this.connection;
    }

    /**
     * Handle closing of SQLConnections
     *
     * @throws SQLException SQLException for error connection
     */
    private void close() throws SQLException {
        if(this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
    }
}
