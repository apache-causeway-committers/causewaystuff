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
package io.github.causewaystuff.tooling.c4;

import java.util.Optional;

import org.springframework.lang.Nullable;

import org.apache.causeway.commons.internal.base._Strings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import io.github.causewaystuff.tooling.structurizr.Workspace;
import io.github.causewaystuff.tooling.structurizr.export.plantuml.StructurizrPlantUMLExporter;
import io.github.causewaystuff.tooling.structurizr.model.Element;
import io.github.causewaystuff.tooling.structurizr.model.Model;
import io.github.causewaystuff.tooling.structurizr.model.Person;
import io.github.causewaystuff.tooling.structurizr.model.SoftwareSystem;
import io.github.causewaystuff.tooling.structurizr.model.Tags;
import io.github.causewaystuff.tooling.structurizr.view.ContainerView;
import io.github.causewaystuff.tooling.structurizr.view.Shape;
import io.github.causewaystuff.tooling.structurizr.view.SystemContextView;
import io.github.causewaystuff.tooling.structurizr.view.ViewSet;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class C4 {

    @Getter private final Workspace workspace;
    private final StructurizrPlantUMLExporter plantUMLExporter;

    /**
     * Creates a new workspace.
     *
     * @param name          the name of the workspace
     * @param description   a short description
     */
    public static C4 of(@NonNull final String name, @Nullable final String description) {
        var plantUMLWriter = new StructurizrPlantUMLExporter();
        var c4 = new C4(new Workspace(name, _Strings.nullToEmpty(description)), plantUMLWriter);
        c4.applyDefaultStyles();
        return c4;
    }

    public String getWorkspaceName() {
        return workspace.getName();
    }

    /**
     * @return the software architecture model
     */
    public Model getModel() {
        return workspace.getModel();
    }

    /**
     * @return set of views onto the software architecture model
     */
    public ViewSet getViewSet() {
        return workspace.getViews();
    }

    /**
     * @return a single {@link ContainerView} as a PlantUML diagram definition
     */
    public String toPlantUML(final ContainerView containerView) {
        return plantUMLExporter.export(containerView).getDefinition();
    }

    /**
     * @return a single {@link SystemContextView} as a PlantUML diagram definition
     */
    public String toPlantUML(final SystemContextView systemContextView) {
        return plantUMLExporter.export(systemContextView).getDefinition();
    }

    // -- SIMPLE FACTORIES

    public Person person(@NonNull final String name, @Nullable final String description) {
        return getModel().addPerson(name, _Strings.nullToEmpty(description));
    }

    public SoftwareSystem softwareSystem(@NonNull final String name, @Nullable final String description) {
        return getModel().addSoftwareSystem(name, _Strings.nullToEmpty(description));
    }

    public SystemContextView systemContextView(@NonNull final SoftwareSystem softwareSystem, @NonNull final String key, @Nullable final String description) {
        return getViewSet().createSystemContextView(softwareSystem, key, _Strings.nullToEmpty(description));
    }

    // -- EXPERIMENTAL

    public static void setTypeOverride(final Element element, final String typeOverride) {
        element.addProperty("typeOverride", typeOverride);
    }

    public static Optional<String> getTypeOverride(final Element element) {
        return Optional.ofNullable(element.getProperties().get("typeOverride"));
    }

    // -- HELPER

    private void applyDefaultStyles() {
        var styles = getViewSet().getConfiguration().getStyles();

        styles.addElementStyle(Tags.ELEMENT).color("#fffffe");
        //styles.addElementStyle(Tags.PERSON).background("#08427b");
        styles.addElementStyle(Tags.CONTAINER).background("#438dd5");

        //        styles.addElementStyle(Tags.SOFTWARE_SYSTEM)
        //        .color("#ffffff")
        //        .background("#1168bd");
        styles.addElementStyle(Tags.PERSON).background("#08427b").color("#ffffff").shape(Shape.Person);
    }

}
