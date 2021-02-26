package net.thenova.titan.module.sqldatabase.tables.column.data_type;

import lombok.RequiredArgsConstructor;
import net.thenova.titan.module.sqldatabase.tables.column.SQLDataType;

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
@RequiredArgsConstructor
public final class VarChar implements SQLDataType {

    public static final int LENGTH_DEFAULT = 100;
    public static final int LENGTH_UUID = 36;
    public static final int LENGTH_NAME = 16;
    public static final int LENGTH_SINGLE = 1;

    private final int length;

    public VarChar() {
        this(LENGTH_DEFAULT);
    }

    @Override
    public final String type() {
        return "varchar(" + length + ")";
    }
}
