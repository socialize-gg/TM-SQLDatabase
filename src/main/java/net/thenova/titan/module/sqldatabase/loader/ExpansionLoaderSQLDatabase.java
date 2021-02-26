package net.thenova.titan.module.sqldatabase.loader;

import net.thenova.titan.Titan;
import net.thenova.titan.module.module.ModuleInstance;
import net.thenova.titan.module.module.expansion.Expansion;
import net.thenova.titan.module.module.expansion.ExpansionLoader;
import net.thenova.titan.module.sqldatabase.sql.SQLConnectionHandler;

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
public final class ExpansionLoaderSQLDatabase implements ExpansionLoader {

    @Override
    public final String name() {
        return "sql-database";
    }

    @Override
    public Class<? extends Expansion> expansion() {
        return ExpansionSQLDatabase.class;
    }

    @Override
    public boolean enable(final ModuleInstance instance, final Expansion expansion) {
        final ExpansionSQLDatabase sqlExpansion = (ExpansionSQLDatabase) expansion;
        if(sqlExpansion.database() == null && sqlExpansion.tables() == null) {
            Titan.INSTANCE.getLogger().debug("[ExpansionLoaderSQLDatabase] - Module '%s' was an instance of SQLDatabaseModule but both database and tables were null", instance.getDescriptionFile().getName());
            return true;
        }

        if(sqlExpansion.tables() != null) {
            SQLConnectionHandler.INSTANCE.createTables(sqlExpansion.tables());
        }
        return true;
    }

    @Override
    public void reload(final ModuleInstance instance, final Expansion expansion) {
        this.enable(instance, expansion);
    }

    @Override
    public void unload(final ModuleInstance instance, final Expansion expansion) {

    }
}
