package net.thenova.titan.module.sqldatabase.tables;

import lombok.Getter;
import net.thenova.titan.module.sqldatabase.sql.SQLExecutor;
import net.thenova.titan.module.sqldatabase.tables.column.TableColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
public abstract class DatabaseTable {

    private final Database database;
    private final String name;

    private final List<TableColumn> columns = new ArrayList<>();

    private String uniqueKey = null;

    public DatabaseTable(final Database database, final String name) {
        this.database = database;
        this.name = name;

        this.init();
    }

    public abstract void init();

    protected final void registerColumn(final TableColumn... c) {
        Arrays.stream(c).forEach(this::registerColumn);
    }

    private void registerColumn(final TableColumn column) {
        this.columns.add(column);
    }

    protected final void addUniqueKey(final String key) {
        this.uniqueKey = key;
    }

    public SQLExecutor build() {
        final StringBuilder rtn = new StringBuilder();
        rtn.append("CREATE TABLE IF NOT EXISTS `").append(this.name).append("` (");

        this.columns.forEach(column -> rtn.append(column.asQuery()).append(","));

        final List<TableColumn> primary;
        if(!(primary = this.columns.stream().filter(TableColumn::isPrimaryKey).collect(Collectors.toList())).isEmpty()) {
            rtn.append("PRIMARY KEY (`").append(primary.stream().map(TableColumn::getName).collect(Collectors.joining("`, `"))).append("`)");
        } else {
            rtn.deleteCharAt(rtn.length() - 1);
        }

        final TableColumn foreign;
        if((foreign = this.columns.stream().filter(TableColumn::isForeign).findFirst().orElse(null)) != null) {
            rtn.append(", FOREIGN KEY (`")
                    .append(foreign.getName())
                    .append("`) REFERENCES ")
                    .append(foreign.getForeignTable())
                    .append("(`")
                    .append(foreign.getForeignField())
                    .append("`)");
        }

        if(this.uniqueKey != null) {
            rtn.append(", UNIQUE KEY(").append(this.uniqueKey).append(")");
        }

        rtn.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");



        return new SQLExecutor(this.database).queryUpdate(rtn.toString());
    }


    /**
     * Perform table creation
     */
    public void create() {
        this.build().commit();
    }
}
