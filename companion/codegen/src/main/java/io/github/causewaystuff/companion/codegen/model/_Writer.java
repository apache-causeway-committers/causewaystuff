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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.commons.io.YamlUtils.YamlWriter;
import org.springframework.util.StringUtils;

import io.github.causewaystuff.companion.codegen.model.Schema.ModuleNaming;
import lombok.experimental.UtilityClass;

@UtilityClass
class _Writer {

    String toYaml(final ModuleNaming naming, final Schema.Domain schema) {
        var yaml = new YamlWriter();
        yaml.write("module:").nl();
        yaml.ind().write("namespace: ", naming.namespace()).nl();
        yaml.ind().write("package: ", naming.javaPackage()).nl();
        yaml.write("viewmodels:").nl();
        for(var viewmodel : schema.viewmodels().values()) {
            writeViewmodel(yaml, viewmodel);
        }
        yaml.write("entities:").nl();
        for(var entity : schema.entities().values()) {
            writeEntity(yaml, entity);
        }
        return yaml.toString();
    }

    String toYaml(final Schema.Entity entity) {
        var yaml = new YamlWriter();
        writeEntity(yaml, entity);
        return yaml.toString();
    }

    void writeViewmodel(final YamlWriter yaml, final Schema.Viewmodel viewmodel) {
        yaml.write("- id: ", viewmodel.id()).nl();
        yaml.ind().write("generator: ", viewmodel.generator()).nl();
        yaml.ind().write("name: ", viewmodel.name()).nl();
        yaml.ind().write("namespace: ", viewmodel.namespace()).nl();
        {   // icon
            var iconLines = TextUtils.readLines(viewmodel.icon());
            if(iconLines.isCardinalityMultiple()) {
            	yaml.multiline(1, "icon", iconLines.toList());
            } else {
                yaml.ind().write("icon: ", viewmodel.icon()).nl();
            }
        }
        if(viewmodel.iconService()) {
            yaml.ind().write("iconService: ", "true").nl();
        }
        if(StringUtils.hasLength(viewmodel.named())) {
            yaml.ind().write("named: ", viewmodel.named()).nl();
        }
        yaml.multiline(1, "description", viewmodel.description().lines());
        yaml.ind().write("fields:").nl();
        viewmodel.fields().forEach(field->writeField(yaml, field));
    }

    void writeField(final YamlWriter yaml, final Schema.VmField field) {
        final var writeLast = new ArrayList<Runnable>(1);
        yaml.ind().ind().write(field.name(), ":").nl();
        yaml.ind().ind().ind().write("type: ", ""+field.type()).nl();
        yaml.ind().ind().ind().write("required: ", ""+field.required()).nl();
        if(field.plural()) {
            yaml.ind().ind().ind().write("plural: ", "true").nl();
        }
        if(_Strings.isNotEmpty(field.elementType())) {
            yaml.ind().ind().ind().write("elementType: ", field.elementType()).nl();
        }
        Optional.ofNullable(field.propertyLayout())
            .ifPresent(propertyLayout->propertyLayout.streamAttributes()
                    .forEach(attr->{
                        if(attr.value() instanceof Multiline ml) {
                            writeLast.add(()->yaml.multiline(3, "description", ml.lines()));
                        } else {
                            yaml.ind().ind().ind().write(attr.name(), ": ", attr.value().toString()).nl();
                        }
                    }));
        if(field.isEnum()) {
        	yaml.multiline(3, "enum", field.enumeration());
        }
        writeLast.forEach(Runnable::run);
    }

    void writeEntity(final YamlWriter yaml, final Schema.Entity entity) {
        yaml.write("- id: ", entity.id()).nl();
        yaml.ind().write("name: ", entity.name()).nl();
        yaml.ind().write("namespace: ", entity.namespace()).nl();
        yaml.ind().write("table: ", entity.table()).nl();
        if(_Strings.isNotEmpty(entity.superType())) {
            yaml.ind().write("superType: ", entity.superType()).nl();
        }
        yaml.multiline(1, "secondaryKey", toUpperCase(entity.secondaryKey()));
        {   // title
            var titleLines = TextUtils.readLines(entity.title());
            if(titleLines.isCardinalityMultiple()) {
                yaml.multiline(1, "title", titleLines.toList());
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
            	yaml.multiline(1, "icon", iconLines.toList());
            } else {
                yaml.ind().write("icon: ", entity.icon()).nl();
            }
        }
        if(entity.iconService()) {
            yaml.ind().write("iconService: ", "true").nl();
        }
        if(StringUtils.hasLength(entity.named())) {
            yaml.ind().write("named: ", entity.named()).nl();
        }
        yaml.multiline(1, "description", entity.description().lines());
        yaml.ind().write("fields:").nl();
        entity.fields().forEach(field->writeField(yaml, field));
    }

	void writeField(final YamlWriter yaml, final Schema.EntityField field) {
        final var writeLast = new ArrayList<Runnable>(1);
        yaml.ind().ind().write(field.name(), ":").nl();
        yaml.ind().ind().ind().write("column: ", field.column()).nl();
        yaml.ind().ind().ind().write("column-type: ", field.columnType()).nl();
        yaml.ind().ind().ind().write("required: ", ""+field.required()).nl();
        yaml.ind().ind().ind().write("unique: ", ""+field.unique()).nl();
        if(field.plural()) {
            yaml.ind().ind().ind().write("plural: ", "true").nl();
        }
        if(_Strings.isNotEmpty(field.elementType())) {
            yaml.ind().ind().ind().write("elementType: ", field.elementType()).nl();
        }
        Optional.ofNullable(field.propertyLayout())
            .ifPresent(propertyLayout->propertyLayout.streamAttributes()
                    .forEach(attr->{
                        if(attr.value() instanceof Multiline ml) {
                            writeLast.add(()->yaml.multiline(3, "description", ml.lines()));
                        } else {
                            yaml.ind().ind().ind().write(attr.name(), ": ", attr.value().toString()).nl();
                        }
                    }));
        if(field.isEnum()) {
            yaml.multiline(3, "enum", field.enumeration());
        }
        if(field.hasDiscriminator()) {
        	yaml.multiline(3, "discriminator", toUpperCase(field.discriminator()));
        }
        if(field.hasForeignKeys()) {
        	yaml.multiline(3, "foreignKeys", toUpperCase(field.foreignKeys()));
        }
        writeLast.forEach(Runnable::run);
    }

	private static List<String> toUpperCase(final List<String> strings) {
		return strings.stream()
        		.map(String::toUpperCase)
        		.toList();
	}

}
