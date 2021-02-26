package net.thenova.titan.module.sqldatabase.loader;

import net.thenova.titan.module.module.expansion.Expansion;
import net.thenova.titan.module.sqldatabase.tables.Database;
import net.thenova.titan.module.sqldatabase.tables.DatabaseTable;

import java.util.List;

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
public interface ExpansionSQLDatabase extends Expansion {

    /**
     * @return - Database being used by the plugin
     */
    Database database();

    /**
     * @return - DatabaseTables
     */
    List<DatabaseTable> tables();
}
