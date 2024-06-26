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

import java.util.HashMap;

import io.github.causewaystuff.tooling.structurizr.Workspace;
import io.github.causewaystuff.tooling.structurizr.dsl.StructurizrDslFormatter;
import io.github.causewaystuff.tooling.structurizr.model.Component;
import io.github.causewaystuff.tooling.structurizr.model.Container;
import io.github.causewaystuff.tooling.structurizr.model.Model;
import io.github.causewaystuff.tooling.structurizr.model.SoftwareSystem;
import io.github.causewaystuff.tooling.structurizr.model.Tags;
import io.github.causewaystuff.tooling.structurizr.view.AutomaticLayout.RankDirection;
import io.github.causewaystuff.tooling.structurizr.view.Shape;
import io.github.causewaystuff.tooling.structurizr.view.Styles;
import io.github.causewaystuff.tooling.structurizr.view.SystemContextView;
import io.github.causewaystuff.tooling.structurizr.view.ViewSet;

import org.apache.causeway.applib.services.metamodel.objgraph.ObjectGraph;

public class ObjectGraphRendererStructurizr implements ObjectGraph.Renderer {

    @Override
    public void render(final StringBuilder sb, final ObjectGraph objGraph) {
        sb.append(StructurizrDslFormatter.toDsl(asWorkspace(objGraph))).append("\n");
    }

    public Workspace asWorkspace(final ObjectGraph objGraph) {
        final Workspace workspace = new Workspace("", "");
        final Model model = workspace.getModel();
        final ViewSet views = workspace.getViews();
        final SoftwareSystem softwareSystem = model
                //TODO yet just sample text
                .addSoftwareSystem("Project xxx Entities", "Software system having all the xxx entities.");

        // maps ObjectGraph.Object(s) to Components(s)
        var containerByName = new HashMap<String, Container>();
        var componentById = new HashMap<String, Component>();
        for(var obj : objGraph.objects()) {
            final Container container =
                    containerByName.computeIfAbsent(obj.packageName(), name->softwareSystem.addContainer(name));

            String name = obj.name();
            String description = obj.description().orElse(null);
            String technology = "Entity";

            var comp = container.addComponent(name, description, technology);
            componentById.put(obj.id(), comp);
        }

        // maps relations to Relationships
        for(var rel : objGraph.relations()) {
            var from = componentById.get(rel.fromId());
            var to   = componentById.get(rel.toId());
            from.uses(to, rel.description());
        }

        // views
        SystemContextView contextView = views
                .createSystemContextView(
                        softwareSystem,
                        "SystemContext",
                        "An example of a System Context diagram.");
        contextView.addAllSoftwareSystems();
        contextView.addAllPeople();

        var cv  = views.createContainerView(softwareSystem, "a0", "a1");
        cv.addAllElements();
        cv.enableAutomaticLayout(RankDirection.TopBottom);

        containerByName.forEach((name, container)->{
            var compView  = views.createComponentView(container, normalizeIdentifier(name), "no desc");

            for (Component component : compView.getContainer().getComponents()) {
                compView.add(component);
                for (Container cnt : compView.getSoftwareSystem().getContainers()) {
                    if (cnt.hasEfferentRelationshipWith(component) || component.hasEfferentRelationshipWith(cnt)) {
                        compView.add(cnt);
                    }
                }
                compView.addNearestNeighbours(component);
            }

            compView.enableAutomaticLayout(RankDirection.TopBottom);
        });

        // add some styling to the diagram elements
        Styles styles = views.getConfiguration().getStyles();
        styles.addElementStyle(Tags.SOFTWARE_SYSTEM).background("#1168bd").color("#ffffff");
        styles.addElementStyle(Tags.PERSON).background("#08427b").color("#ffffff").shape(Shape.Person);

        return workspace;
    }

    private static String normalizeIdentifier(final String name) {
        return name.replace('.', '0');
    }

}
