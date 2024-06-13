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

import java.util.List;

import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.TextUtils;

import lombok.val;
import lombok.experimental.UtilityClass;

@UtilityClass
class _Writer {

    String toYaml(final OrmModel.Schema schema) {
        val yaml = new YamlWriter();
        for(val entity : schema.entities().values()) {
            writeEntity(yaml, entity);
        }
        return yaml.toString();
    }

    String toYaml(final OrmModel.Entity entity) {
        val yaml = new YamlWriter();
        writeEntity(yaml, entity);
        return yaml.toString();
    }

    void writeEntity(final YamlWriter yaml, final OrmModel.Entity entity) {
        yaml.write(entity.key(), ":").nl();
        yaml.ind().write("namespace: ", entity.namespace()).nl();
        yaml.ind().write("table: ", entity.table()).nl();
        if(_Strings.isNotEmpty(entity.superType())) {
            yaml.ind().write("superType: ", entity.superType()).nl();
        }
        yaml.ind().write("secondaryKey:").multiLineStartIfNotEmtpy(entity.secondaryKey()).nl();
        entity.secondaryKey().forEach(line->
        yaml.ind().ind().writeUpper(line).nl());
        {   // title
            var titleLines = TextUtils.readLines(entity.title());
            if(titleLines.isCardinalityMultiple()) {
                yaml.ind().write("title:").multiLineStartIfNotEmtpy(titleLines.toList()).nl();
                titleLines.forEach(line->
                yaml.ind().ind().write(line).nl());
            } else {
                yaml.ind().write("title: ", entity.title()).nl();
            }
        }
        if(entity.suppressUniqueConstraint()) {
            yaml.ind().write("suppressUniqueConstraint: ", "true").nl();
        }
        {   // icon
            var iconLines = TextUtils.readLines(entity.icon());
            if(iconLines.isCardinalityMultiple()) {
                yaml.ind().write("icon:").multiLineStartIfNotEmtpy(iconLines.toList()).nl();
                iconLines.forEach(line->
                yaml.ind().ind().write(line).nl());
            } else {
                yaml.ind().write("icon: ", entity.icon()).nl();
            }
        }
        if(entity.iconService()) {
            yaml.ind().write("iconService: ", "true").nl();
        }
        yaml.ind().write("description:").multiLineStartIfNotEmtpy(entity.description()).nl();
        entity.description().forEach(line->
        yaml.ind().ind().write(line).nl());
        yaml.ind().write("fields:").nl();
        entity.fields().forEach(field->writeField(yaml, field));
    }

    void writeField(final YamlWriter yaml, final OrmModel.Field field) {
        yaml.ind().ind().write(field.name(), ":").nl();
        yaml.ind().ind().ind().write("column: ", field.column()).nl();
        yaml.ind().ind().ind().write("column-type: ", field.columnType()).nl();
        yaml.ind().ind().ind().write("required: ", ""+field.required()).nl();
        yaml.ind().ind().ind().write("unique: ", ""+field.unique()).nl();
        if(field.plural()) {
            yaml.ind().ind().ind().write("plural: ", "true").nl();
        }
        if(field.multiLine().isPresent()) {
            yaml.ind().ind().ind().write("multiLine: ", ""+field.multiLine().getAsInt()).nl();
        }
        if(_Strings.isNotEmpty(field.elementType())) {
            yaml.ind().ind().ind().write("elementType: ", field.elementType()).nl();
        }
        if(field.hiddenWhere()!=null) {
            yaml.ind().ind().ind().write("hiddenWhere: ", field.hiddenWhere().name()).nl();
        }
        if(field.isEnum()) {
            yaml.ind().ind().ind().write("enum:").multiLineStartIfNotEmtpy(field.enumeration()).nl();
            field.enumeration().forEach(line->
            yaml.ind().ind().ind().ind().write(line).nl());
        }
        if(field.hasDiscriminator()) {
            yaml.ind().ind().ind().write("discriminator:").multiLineStartIfNotEmtpy(field.discriminator()).nl();
            field.discriminator().forEach(line->
            yaml.ind().ind().ind().ind().writeUpper(line).nl());
        }
        if(field.hasForeignKeys()) {
            yaml.ind().ind().ind().write("foreignKeys:").multiLineStartIfNotEmtpy(field.foreignKeys()).nl();
            field.foreignKeys().forEach(line->
            yaml.ind().ind().ind().ind().writeUpper(line).nl());
        }
        yaml.ind().ind().ind().write("description:").multiLineStartIfNotEmtpy(field.description()).nl();
        field.description().forEach(line->
        yaml.ind().ind().ind().ind().write(line).nl());
    }

    // -- HELPER

    static class YamlWriter {
        final StringBuilder sb = new StringBuilder();
        @Override public String toString() { return sb.toString(); }
        YamlWriter multiLineStartIfNotEmtpy(final List<?> list) {
            if(!_NullSafe.isEmpty(list)) sb.append(" |");
            return this;
        }
        YamlWriter write(final String ...s) {
            for(val str:s) sb.append(str);
            return this;
        }
        YamlWriter writeUpper(final String ...s) {
            for(val str:s) sb.append(str.toUpperCase());
            return this;
        }
        YamlWriter ind() {
            sb.append("  ");
            return this;
        }
        YamlWriter nl() {
            sb.append('\n');
            return this;
        }
    }

}
