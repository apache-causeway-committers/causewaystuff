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
import io.github.causewaystuff.tooling.structurizr.dsl.StructurizrDslFormatter;
import io.github.causewaystuff.tooling.structurizr.dsl.StructurizrDslParser;
import io.github.causewaystuff.tooling.structurizr.model.Container;
import io.github.causewaystuff.tooling.structurizr.model.InteractionStyle;
import io.github.causewaystuff.tooling.structurizr.model.Model;
import io.github.causewaystuff.tooling.structurizr.model.Person;
import io.github.causewaystuff.tooling.structurizr.model.SoftwareSystem;
import io.github.causewaystuff.tooling.structurizr.model.Tags;
import io.github.causewaystuff.tooling.structurizr.view.ContainerView;
import io.github.causewaystuff.tooling.structurizr.view.DynamicView;
import io.github.causewaystuff.tooling.structurizr.view.Routing;
import io.github.causewaystuff.tooling.structurizr.view.Shape;
import io.github.causewaystuff.tooling.structurizr.view.Styles;
import io.github.causewaystuff.tooling.structurizr.view.SystemContextView;
import io.github.causewaystuff.tooling.structurizr.view.ViewSet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import lombok.SneakyThrows;

class WorkspaceDslGenTest {

    enum Scenario {
        /**
         * @see "https://github.com/structurizr/examples/blob/main/java/src/main/java/com/structurizr/example/GettingStarted.java"
         */
        GETTING_STARTED {
            @Override public Workspace workspace() {
                // all software architecture models belong to a workspace
                var workspace = new Workspace("Getting Started", "This is a model of my software system.");
                Model model = workspace.getModel();

                // create a model to describe a user using a software system
                Person user = model.addPerson("User", "A user of my software system.");
                SoftwareSystem softwareSystem = model.addSoftwareSystem("Software System", "My software system.");
                user.uses(softwareSystem, "Uses");

                // create a system context diagram showing people and software systems
                ViewSet views = workspace.getViews();
                SystemContextView contextView = views.createSystemContextView(softwareSystem, "SystemContext", "An example of a System Context diagram.");
                contextView.addAllSoftwareSystems();
                contextView.addAllPeople();

                // add some styling to the diagram elements
                Styles styles = views.getConfiguration().getStyles();
                styles.addElementStyle(Tags.SOFTWARE_SYSTEM).background("#1168bd").color("#ffffff");
                styles.addElementStyle(Tags.PERSON).background("#08427b").color("#ffffff").shape(Shape.Person);

                return workspace;
            }
        },
        /**
         * @see "https://docs.structurizr.com/dsl/cookbook/container-view/"
         */
        CONTAINER_VIEW {
            @SneakyThrows
            @Override public Workspace workspace() {
                var dsl =
                """
                workspace {
                    model {
                        u = person "User"
                        s = softwareSystem "Software System" {
                            webapp = container "Web Application"
                            database = container "Database"
                        }

                        u -> webapp "Uses"
                        webapp -> database "Reads from and writes to"
                    }
                    views {
                        container s {
                            include *
                            autoLayout lr
                        }
                    }
                }
                """;
                var parser = new StructurizrDslParser();
                parser.parse(dsl);
                return parser.getWorkspace();
            }
        },
        /**
         * @see "https://docs.structurizr.com/dsl/cookbook/dynamic-view-parallel/"
         */
        DYNAMIC_VIEW {
            @SneakyThrows
            @Override public Workspace workspace() {
                var dsl =
                """
                workspace {
                    model {
                        a = softwareSystem "A"
                        b = softwareSystem "B"
                        c = softwareSystem "C"
                        d = softwareSystem "D"
                        e = softwareSystem "E"

                        a -> b
                        b -> c
                        b -> d
                        b -> e
                    }
                    views {

                        dynamic * {
                            a -> b "Makes a request to"
                            {
                                {
                                    b -> c "Gets data from"
                                }
                                {
                                    b -> d "Gets data from"
                                }
                            }
                            b -> e "Sends data to"

                            autoLayout
                        }
                    }
                }

                """;
                var parser = new StructurizrDslParser();
                parser.parse(dsl);
                return parser.getWorkspace();
            }
        },
        /**
         * @see "https://github.com/structurizr/examples/blob/main/java/src/main/java/com/structurizr/example/MicroservicesExample.java"
         */
        MICROSERVICES {
            @Override public Workspace workspace() {
                final String MICROSERVICE_TAG = "Microservice";
                final String MESSAGE_BUS_TAG = "Message Bus";
                final String DATASTORE_TAG = "Database";

                Workspace workspace = new Workspace("Microservices example", "An example of a microservices architecture, which includes asynchronous and parallel behaviour.");
                Model model = workspace.getModel();

                SoftwareSystem mySoftwareSystem = model.addSoftwareSystem("Customer Information System", "Stores information ");
                Person customer = model.addPerson("Customer", "A customer");
                Container customerApplication = mySoftwareSystem.addContainer("Customer Application", "Allows customers to manage their profile.", "Angular");

                Container customerService = mySoftwareSystem.addContainer("Customer Service", "The point of access for customer information.", "Java and Spring Boot");
                customerService.addTags(MICROSERVICE_TAG);
                Container customerDatabase = mySoftwareSystem.addContainer("Customer Database", "Stores customer information.", "Oracle 12c");
                customerDatabase.addTags(DATASTORE_TAG);

                Container reportingService = mySoftwareSystem.addContainer("Reporting Service", "Creates normalised data for reporting purposes.", "Ruby");
                reportingService.addTags(MICROSERVICE_TAG);
                Container reportingDatabase = mySoftwareSystem.addContainer("Reporting Database", "Stores a normalised version of all business data for ad hoc reporting purposes.", "MySQL");
                reportingDatabase.addTags(DATASTORE_TAG);

                Container auditService = mySoftwareSystem.addContainer("Audit Service", "Provides organisation-wide auditing facilities.", "C# .NET");
                auditService.addTags(MICROSERVICE_TAG);
                Container auditStore = mySoftwareSystem.addContainer("Audit Store", "Stores information about events that have happened.", "Event Store");
                auditStore.addTags(DATASTORE_TAG);

                Container messageBus = mySoftwareSystem.addContainer("Message Bus", "Transport for business events.", "RabbitMQ");
                messageBus.addTags(MESSAGE_BUS_TAG);

                customer.uses(customerApplication, "Uses");
                customerApplication.uses(customerService, "Updates customer information using", "JSON/HTTPS", InteractionStyle.Synchronous);
                customerService.uses(messageBus, "Sends customer update events to", "", InteractionStyle.Asynchronous);
                customerService.uses(customerDatabase, "Stores data in", "JDBC", InteractionStyle.Synchronous);
                customerService.uses(customerApplication, "Sends events to", "WebSocket", InteractionStyle.Asynchronous);
                messageBus.uses(reportingService, "Sends customer update events to", "", InteractionStyle.Asynchronous);
                messageBus.uses(auditService, "Sends customer update events to", "", InteractionStyle.Asynchronous);
                reportingService.uses(reportingDatabase, "Stores data in", "", InteractionStyle.Synchronous);
                auditService.uses(auditStore, "Stores events in", "", InteractionStyle.Synchronous);

                ViewSet views = workspace.getViews();

                ContainerView containerView = views.createContainerView(mySoftwareSystem, "Containers", null);
                containerView.addAllElements();

                DynamicView dynamicView = views.createDynamicView(mySoftwareSystem, "CustomerUpdateEvent", "This diagram shows what happens when a customer updates their details.");
                dynamicView.add(customer, customerApplication);
                dynamicView.add(customerApplication, customerService);

                dynamicView.add(customerService, customerDatabase);
                dynamicView.add(customerService, messageBus);

                dynamicView.startParallelSequence();
                dynamicView.add(messageBus, reportingService);
                dynamicView.add(reportingService, reportingDatabase);
                dynamicView.endParallelSequence();

                dynamicView.startParallelSequence();
                dynamicView.add(messageBus, auditService);
                dynamicView.add(auditService, auditStore);
                dynamicView.endParallelSequence();

                dynamicView.startParallelSequence();
                dynamicView.add(customerService, "Confirms update to", customerApplication);
                dynamicView.endParallelSequence();

                Styles styles = views.getConfiguration().getStyles();
                styles.addElementStyle(Tags.ELEMENT).color("#000000");
                styles.addElementStyle(Tags.PERSON).background("#ffbf00").shape(Shape.Person);
                styles.addElementStyle(Tags.CONTAINER).background("#facc2E");
                styles.addElementStyle(MESSAGE_BUS_TAG).width(1600).shape(Shape.Pipe);
                styles.addElementStyle(MICROSERVICE_TAG).shape(Shape.Hexagon);
                styles.addElementStyle(DATASTORE_TAG).background("#f5da81").shape(Shape.Cylinder);
                styles.addRelationshipStyle(Tags.RELATIONSHIP).routing(Routing.Orthogonal);

                styles.addRelationshipStyle(Tags.ASYNCHRONOUS).dashed(true);
                styles.addRelationshipStyle(Tags.SYNCHRONOUS).dashed(false);

                return workspace;
            }
        }
        ;
        public abstract Workspace workspace();
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void roundtrip(final Scenario scenario) {
        if(scenario==Scenario.MICROSERVICES) return; //TODO broken scenario
        assertDslRoundtrip(scenario.workspace());
    }

    @SneakyThrows
    private void assertDslRoundtrip(final Workspace workspace) {
        var generatedDsl = new StructurizrDslFormatter().format(workspace);

        System.err.printf("%s%n", generatedDsl);

        //System.err.printf("%s%n", WorkspaceUtils.toJson(workspace, true));

        var parser = new StructurizrDslParser();
        parser.parse(generatedDsl);
        _WorkspaceEquality.assertWorkspaceEquals(workspace, parser.getWorkspace());
    }

}
