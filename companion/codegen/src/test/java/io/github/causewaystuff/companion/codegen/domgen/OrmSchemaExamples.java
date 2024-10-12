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
package io.github.causewaystuff.companion.codegen.domgen;

import java.util.ArrayList;
import java.util.List;

import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.commons.collections.Can;

import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.types.internal.ObjectRef;
import io.github.causewaystuff.commons.base.types.internal.SneakyRef;
import io.github.causewaystuff.companion.codegen.model.Multiline;
import io.github.causewaystuff.companion.codegen.model.PropertyLayoutSpec;
import io.github.causewaystuff.companion.codegen.model.Schema;
import io.github.causewaystuff.companion.codegen.model.Schema.Domain;
import io.github.causewaystuff.companion.codegen.model.Schema.Entity;
import io.github.causewaystuff.companion.codegen.model.Schema.EntityField;

@UtilityClass
public class OrmSchemaExamples {

    public Can<Domain> examples() {
        var entity = new Entity(
                ObjectRef.empty(),
                "Customer", "causewaystuff", "FOODS", "", List.of(), false, "name", "fa-pencil",
                false,
                "named",
                List.of("Customer List and Aliases"),
                new ArrayList<Schema.EntityField>());
        var field = new EntityField(SneakyRef.of(entity), /*ordinal*/0, "name", "NAME", "nvarchar(100)",
                true, false, false,
                "",
                PropertyLayoutSpec.builder()
                    .multiLine(2)
                    .hiddenWhere(Where.ALL_TABLES)
                    .build(),
                List.of(), List.of(), List.of(), new Multiline(List.of("aa", "bb", "cc")));
        entity.fields().add(field);
        return Can.of(
                Domain.of(List.of(),List.of(entity)));
    }

}
