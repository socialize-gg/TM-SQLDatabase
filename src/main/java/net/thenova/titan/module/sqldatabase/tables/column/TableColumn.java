package net.thenova.titan.module.sqldatabase.tables.column;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
@Getter
@RequiredArgsConstructor
public final class TableColumn {

    private final String name;
    private final SQLDataType type;

    private String defaultValue = null;

    private boolean nullable = false;
    private boolean primaryKey = false;

    private String foreignTable = null;
    private String foreignField = null;

    /**
     * Used to make the current column the Primary Key for the table
     *
     * @return - TableColumn
     */
    public final TableColumn setPrimary() {
        this.primaryKey = true;

        return this;
    }

    /**
     * Used to add the current column as a foreign key of another table
     *
     * @param table - String
     * @param field - String
     * @return - TableColumn
     */
    public final TableColumn foreign(final String table, final String field) {
        this.foreignTable = table;
        this.foreignField = field;

        return this;
    }

    /**
     * Make it so the column is nullable
     *
     * @return - TableColumn
     */
    public final TableColumn setNullable() {
        this.nullable = true;

        return this;
    }

    /**
     * Set a default value for the column, used for inserting without a value
     *
     * @param val - Object
     * @return - TableColumn
     */
    public final TableColumn setDefault(final Object val) {
        this.defaultValue = "'" + val.toString() + "'";

        return this;
    }

    /**
     * @return - Boolean
     */
    public final boolean isForeign() {
        return this.foreignTable != null && this.foreignField != null;
    }

    /**
     * Build query for specific SQL column, for use building table
     *
     * @return - String
     */
    public final String asQuery() {
        return "`" + this.name + "` "
                + this.type.type() + " "
                + (this.nullable ? "" : "NOT") + " NULL"
                + (this.defaultValue != null ? " DEFAULT " + this.defaultValue : "");
    }
}
