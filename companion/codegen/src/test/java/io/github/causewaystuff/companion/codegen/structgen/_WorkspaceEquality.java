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
package io.github.causewaystuff.companion.codegen.structgen;

import io.github.causewaystuff.tooling.structurizr.Workspace;
import io.github.causewaystuff.tooling.structurizr.model.Element;
import io.github.causewaystuff.tooling.structurizr.model.Model;
import io.github.causewaystuff.tooling.structurizr.model.Relationship;
import io.github.causewaystuff.tooling.structurizr.view.View;
import io.github.causewaystuff.tooling.structurizr.view.ViewSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
class _WorkspaceEquality {

    @SneakyThrows
    void assertWorkspaceEquals(final Workspace a, final Workspace b) {

//        assertEquals(
//                WorkspaceUtils.toJson(a, true),
//                WorkspaceUtils.toJson(b, true));

        assertEquals(a.getName(), b.getName());
        assertEquals(a.getDescription(), b.getDescription());
        assertEquals(a.getVersion(), b.getVersion());
        assertModelEquals(a.getModel(), b.getModel());
        assertViewsEquals(a.getViews(), b.getViews());
    }

    void assertModelEquals(final Model a, final Model b) {
        var ela = a.getElements();
        var elb = b.getElements();
        var rela = a.getRelationships();
        var relb = b.getRelationships();
        assertEquals(ela.size(), elb.size(), ()->"structurizr model elements differ in size");
        assertEquals(rela.size(), relb.size(), ()->"structurizr model relations differ in size");
        ela.stream().forEach(element->assertElementEquals(element, b.getElement(element.getId())));
        rela.stream().forEach(rel->assertRelationshipEquals(rel, b.getRelationship(rel.getId())));
    }

    void assertRelationshipEquals(final Relationship a, final Relationship b) {
        assertEquals(a.getSourceId(), b.getSourceId());
        assertEquals(a.getDestinationId(), b.getDestinationId());
        assertEquals(a.getDescription(), b.getDescription());
    }

    void assertViewsEquals(final ViewSet a, final ViewSet b) {
        var va = a.getViews();
        var vb = b.getViews();
        assertEquals(va.size(), vb.size(), ()->"structurizr views differ in size");
        va.stream().forEach(view->assertViewEquals(view, b.getViewWithKey(view.getKey())));
    }

    void assertViewEquals(final View a, final View b) {
        assertEquals(a.getKey(), b.getKey());
        assertEquals(a.getDescription(), b.getDescription());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getTitle(), b.getTitle());
        assertEquals(a.getClass(), b.getClass());
    }

    void assertElementEquals(final Element a, final Element b) {
        assertEquals(a.getId(), b.getId());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getDescription(), b.getDescription());
    }

}
