/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.github.causewaystuff.companion.codegen.model;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.assertions._Assert;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.exceptions._Exceptions;

import lombok.val;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.companion.codegen.model.OrmModel.Entity;
import io.github.causewaystuff.companion.codegen.model.OrmModel.Field;
import io.github.causewaystuff.companion.codegen.model.OrmModel.Schema;

@UtilityClass
class _Foreign {

    Can<Entity> findEntitiesWithoutRelations(final Schema schema){
        val foreignKeyFields = // as table.column literal
                schema.entities().values().stream().flatMap(fe->fe.fields().stream())
                    .flatMap(ff->ff.foreignKeys().stream())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        val entitiesWithoutRelations =  schema.entities().values().stream()
            .filter(e->!e.fields().stream().anyMatch(f->f.hasForeignKeys()))
            .filter(e->!e.fields().stream().anyMatch(f->
                foreignKeyFields.contains(e.table().toLowerCase() + "." + f.column().toLowerCase())))
            .sorted((a, b)->_Strings.compareNullsFirst(a.name(), b.name()))
            .collect(Can.toCan());
        return entitiesWithoutRelations;
    }

    Can<Field> foreignFields(final Field field) {
        final Schema schema = field.parentEntity().parentSchema();
        return field.foreignKeys().stream()
                .map(tableDotCom->lookupForeignKeyFieldElseFail(schema, tableDotCom))
                .collect(Can.toCan());
    }

    // -- HELPER

    private OrmModel.Field lookupForeignKeyFieldElseFail(final Schema schema, final String tableDotColumn) {
        return lookupForeignKeyField(schema, tableDotColumn)
                .orElseThrow(()->_Exceptions.noSuchElement("foreign key not found '%s'", tableDotColumn));
    }

    private Optional<OrmModel.Field> lookupForeignKeyField(final Schema schema, final String tableDotColumn) {
        val parts = _Strings.splitThenStream(tableDotColumn, ".")
                .collect(Can.toCan());
        _Assert.assertEquals(2, parts.size(), ()->String.format(
                "could not parse foreign key '%s'", tableDotColumn));
        val tableName = parts.getElseFail(0);
        val columnName = parts.getElseFail(1);
        return schema.lookupEntityByTableName(tableName)
                .flatMap(entity->entity.lookupFieldByColumnName(columnName));
    }

}
