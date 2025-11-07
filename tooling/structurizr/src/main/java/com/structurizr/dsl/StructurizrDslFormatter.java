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
package com.structurizr.dsl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.structurizr.Workspace;
import com.structurizr.model.Component;
import com.structurizr.model.Container;
import com.structurizr.model.ContainerInstance;
import com.structurizr.model.DeploymentElement;
import com.structurizr.model.DeploymentNode;
import com.structurizr.model.Element;
import com.structurizr.model.InfrastructureNode;
import com.structurizr.model.Location;
import com.structurizr.model.Model;
import com.structurizr.model.ModelItem;
import com.structurizr.model.Person;
import com.structurizr.model.Perspective;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.model.SoftwareSystemInstance;
import com.structurizr.util.StringUtils;
import com.structurizr.view.AutomaticLayout;
import com.structurizr.view.ComponentView;
import com.structurizr.view.ContainerView;
import com.structurizr.view.DeploymentView;
import com.structurizr.view.DynamicView;
import com.structurizr.view.ElementStyle;
import com.structurizr.view.ElementView;
import com.structurizr.view.FilteredView;
import com.structurizr.view.Font;
import com.structurizr.view.RelationshipStyle;
import com.structurizr.view.RelationshipView;
import com.structurizr.view.SystemContextView;
import com.structurizr.view.SystemLandscapeView;
import com.structurizr.view.ViewSet;

import lombok.SneakyThrows;

/**
 * Formats a Structurizr {@link Workspace} to DSL.
 * This implementation isn't perfect, but it should help anybody wanting to transition to the DSL.
 * <p>
 * Resurrected from the dead.
 * @see "https://github.com/structurizr/json/discussions/7"
 */
public final class StructurizrDslFormatter extends StructurizrDslTokens {

    @SneakyThrows
    public static String toDsl(final Workspace workspace) {
        return new StructurizrDslFormatter().format(workspace);
    }

    private static final String DEFAULT_FONT = "Open Sans";

    private StringBuilder buf = new StringBuilder();
    private int indent = 0;

    /**
     * Formats the specified Structurizr {@link Workspace} to the Structurizr DSL.
     *
     * @throws StructurizrDslFormatterException if the workspace can't be formatted
     * @return a DSL representation of the workspace
     */
    public String format(final Workspace workspace) throws StructurizrDslFormatterException {

        Model model = workspace.getModel();

        start(WORKSPACE_TOKEN, quote(workspace.getName()), quote(workspace.getDescription()));
        newline();

        start(IMPLIED_RELATIONSHIPS_TOKEN, quote("false"));
        end();
        start(IDENTIFIERS_TOKEN, quote("hierarchical"));
        end();
        newline();

        start(MODEL_TOKEN);

//        List<Person> internalPeople = model.getPeople().stream().sorted(Comparator.comparing(Person::getId)).collect(Collectors.toList());
//        List<SoftwareSystem> internalSoftwareSystems = model.getSoftwareSystems().stream().sorted(Comparator.comparing(SoftwareSystem::getId)).collect(Collectors.toList());

//        if (workspace.getModel().getEnterprise() == null) {
//            workspace.getModel().setEnterprise(new Enterprise("Enterprise"));
//        }

//        if (workspace.getModel().getEnterprise() != null
//                && (!internalPeople.isEmpty()
//                || !internalSoftwareSystems.isEmpty())) {
//            start(ENTERPRISE_TOKEN, quote(workspace.getModel().getEnterprise().getName()));
//
//            internalPeople.forEach(this::format);
//            internalSoftwareSystems.forEach(this::format);
//
//            end();
//        }

        model.getPeople().stream().sorted(Comparator.comparing(Person::getId)).forEach(this::format);
        model.getSoftwareSystems().stream().sorted(Comparator.comparing(SoftwareSystem::getId)).forEach(this::format);

        model.getRelationships().stream().sorted(Comparator.comparing(Relationship::getId)).forEach(r -> {
            if (r.getSource() instanceof DeploymentElement || r.getDestination() instanceof DeploymentElement) {
                // deployment element relationships are formatted below, after the deployment nodes have been formatted
            } else {
                start(id(r.getSource(), true), RELATIONSHIP_TOKEN, id(r.getDestination(), true), quote(r.getDescription()), quote(r.getTechnology()), quote(tags(r)));
                formatModelItem(r);
                end();
            }
        });

        if (!workspace.getModel().getDeploymentNodes().isEmpty()) {
            newline();
            model.getDeploymentNodes().stream().map(DeploymentElement::getEnvironment).collect(Collectors.toSet()).stream().sorted().forEach(deploymentEnvironment -> {
                start(filter(deploymentEnvironment), ASSIGNMENT_OPERATOR_TOKEN, DEPLOYMENT_ENVIRONMENT_TOKEN, quote(deploymentEnvironment));
                model.getDeploymentNodes().stream().filter(dn -> dn.getParent() == null && dn.getEnvironment().equals(deploymentEnvironment)).sorted(Comparator.comparing(DeploymentNode::getId)).forEach(this::format);
                end();
            });
            newline();

            model.getRelationships().stream().sorted(Comparator.comparing(Relationship::getId)).forEach(r -> {
                if (StringUtils.isNullOrEmpty(r.getLinkedRelationshipId())) {
                    if (r.getSource() instanceof DeploymentElement || r.getDestination() instanceof DeploymentElement) {
                        start(id(r.getSource(), true), RELATIONSHIP_TOKEN, id(r.getDestination(), true), quote(r.getDescription()), quote(r.getTechnology()), quote(tags(r)));
                        formatModelItem(r);
                        end();
                    } else {
                        // static structure element only relationships are formatted above
                    }
                } else {
                    // do nothing - linked relationships are created automatically
                }
            });
        }

        end();

        boolean hasViews = !workspace.getViews().isEmpty();
        boolean hasStyles = !workspace.getViews().getConfiguration().getStyles().getElements().isEmpty() || !workspace.getViews().getConfiguration().getStyles().getRelationships().isEmpty();
        boolean hasThemes = workspace.getViews().getConfiguration().getThemes() != null && workspace.getViews().getConfiguration().getThemes().length > 0;
        boolean hasBranding = workspace.getViews().getConfiguration().getBranding() != null && (!StringUtils.isNullOrEmpty(workspace.getViews().getConfiguration().getBranding().getLogo()) || workspace.getViews().getConfiguration().getBranding().getFont() != null);

        if (hasViews || hasStyles || hasThemes || hasBranding) {
            newline();
            start(VIEWS_TOKEN);

            ViewSet views = workspace.getViews();

            views.getSystemLandscapeViews().stream().sorted(Comparator.comparing(SystemLandscapeView::getKey)).forEach(view -> {
                if (view.getElements().size() > 0 || view.getRelationships().size() > 0) {
                    start(SYSTEM_LANDSCAPE_VIEW_TOKEN, quote(view.getKey().replaceAll(" ", "")), quote(view.getDescription()));

                    view.getElements().stream().map(ElementView::getElement).sorted(Comparator.comparing(Element::getId)).forEach(e ->
                    {
                        start(INCLUDE_IN_VIEW_TOKEN, id(e, true));
                        end();
                    });

                    format(view.getAutomaticLayout());

                    end();
                    newline();
                }
            });

            views.getSystemContextViews().stream().sorted(Comparator.comparing(SystemContextView::getKey)).forEach(view -> {
                if (view.getElements().size() > 0 || view.getRelationships().size() > 0) {
                    start(SYSTEM_CONTEXT_VIEW_TOKEN, id(view.getSoftwareSystem()), quote(view.getKey().replaceAll(" ", "")), quote(view.getDescription()));

                    view.getElements().stream().map(ElementView::getElement).sorted(Comparator.comparing(Element::getId)).forEach(e ->
                    {
                        start(INCLUDE_IN_VIEW_TOKEN, id(e, true));
                        end();
                    });

                    format(view.getAutomaticLayout());

                    end();
                    newline();
                }
            });

            views.getContainerViews().stream().sorted(Comparator.comparing(ContainerView::getKey)).forEach(view -> {
                if (view.getElements().size() > 0 || view.getRelationships().size() > 0) {
                    start(CONTAINER_VIEW_TOKEN, id(view.getSoftwareSystem()), quote(view.getKey().replaceAll(" ", "")), quote(view.getDescription()));

                    view.getElements().stream().map(ElementView::getElement).sorted(Comparator.comparing(Element::getId)).forEach(e ->
                    {
                        start(INCLUDE_IN_VIEW_TOKEN, id(e, true));
                        end();
                    });

                    format(view.getAutomaticLayout());

                    end();
                    newline();
                }
            });

            views.getComponentViews().stream().sorted(Comparator.comparing(ComponentView::getKey)).forEach(view -> {
                if (view.getElements().size() > 0 || view.getRelationships().size() > 0) {
                    start(COMPONENT_VIEW_TOKEN, id(view.getContainer(), true), quote(view.getKey().replaceAll(" ", "")), quote(view.getDescription()));

                    view.getElements().stream().map(ElementView::getElement).sorted(Comparator.comparing(Element::getId)).forEach(e ->
                    {
                        start(INCLUDE_IN_VIEW_TOKEN, id(e, true));
                        end();
                    });

                    format(view.getAutomaticLayout());

                    end();
                    newline();
                }
            });

            views.getFilteredViews().stream().sorted(Comparator.comparing(FilteredView::getKey)).forEach(view -> {
                StringBuilder tags = new StringBuilder();
                for (String tag : view.getTags()) {
                    tags.append(tag);
                    tags.append(" ");
                }

                start(FILTERED_VIEW_TOKEN, quote(view.getBaseViewKey().replaceAll(" ", "")), view.getMode().toString(), quote(tags.toString().trim()), quote(view.getKey().replaceAll(" ", "")), quote(view.getDescription()));
                end();
                newline();
            });

            views.getDynamicViews().stream().sorted(Comparator.comparing(DynamicView::getKey)).forEach(view -> {
                if (view.getElements().size() > 0 || view.getRelationships().size() > 0) {
                    if (StringUtils.isNullOrEmpty(view.getElementId())) {
                        start(DYNAMIC_VIEW_TOKEN, quote("*"), quote(view.getKey()), quote(view.getDescription()));
                    } else {
                        start(DYNAMIC_VIEW_TOKEN, id(view.getElement(), true), quote(view.getKey()), quote(view.getDescription()));
                    }

                    for (RelationshipView relationshipView : view.getRelationships()) {
                        start("# " + relationshipView.getOrder());
                        end();

                        Element source;
                        Element destination;

                        if (relationshipView.isResponse() != null && relationshipView.isResponse()) {
                            source = relationshipView.getRelationship().getDestination();
                            destination = relationshipView.getRelationship().getSource();
                        } else {
                            source = relationshipView.getRelationship().getSource();
                            destination = relationshipView.getRelationship().getDestination();
                        }

                        if (StringUtils.isNullOrEmpty(relationshipView.getDescription())) {
                            start(id(source, true), "->", id(destination, true));
                        } else {
                            start(id(source, true), "->", id(destination, true), quote(relationshipView.getDescription()));
                        }
                        end();
                    }

                    format(view.getAutomaticLayout());

                    end();
                    newline();
                }
            });

            views.getDeploymentViews().stream().sorted(Comparator.comparing(DeploymentView::getKey)).forEach(view -> {
                if (view.getElements().size() > 0 || view.getRelationships().size() > 0) {
                    String scope;

                    if (StringUtils.isNullOrEmpty(view.getSoftwareSystemId())) {
                        scope = "*";
                    } else {
                        scope = id(view.getSoftwareSystem());
                    }

                    start(DEPLOYMENT_VIEW_TOKEN, scope, quote(view.getEnvironment()), quote(view.getKey()), quote(view.getDescription()));

                    Set<Element> elements = new LinkedHashSet<>();
                    for (ElementView elementView : view.getElements()) {
                        DeploymentElement deploymentElement = (DeploymentElement) elementView.getElement();

                        if (deploymentElement instanceof DeploymentNode) {
                            // ignore
                        } else {
                            elements.add(deploymentElement.getParent());
                        }
                    }

                    for (Element element : elements) {
                        start(INCLUDE_IN_VIEW_TOKEN, id(element, true));
                        end();
                    }

                    format(view.getAutomaticLayout());

                    end();
                    newline();
                }
            });

            if (hasStyles) {
                start(STYLES_TOKEN);
                workspace.getViews().getConfiguration().getStyles().getElements().stream().sorted(Comparator.comparing(ElementStyle::getTag)).forEach(style -> {
                    boolean hasProperties = false;
                    start(ELEMENT_STYLE_TOKEN, quote(style.getTag()));

                    if (style.getShape() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_SHAPE_TOKEN, quote(style.getShape()));
                        end();
                    }

                    if (!StringUtils.isNullOrEmpty(style.getIcon())) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_ICON_TOKEN, quote(style.getIcon()));
                        end();
                    }

                    if (style.getWidth() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_WIDTH_TOKEN, quote(style.getWidth()));
                        end();
                    }

                    if (style.getHeight() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_HEIGHT_TOKEN, quote(style.getHeight()));
                        end();
                    }

                    if (!StringUtils.isNullOrEmpty(style.getBackground())) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_BACKGROUND_TOKEN, quote(style.getBackground()));
                        end();
                    }

                    if (!StringUtils.isNullOrEmpty(style.getColor())) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_COLOR_TOKEN, quote(style.getColor()));
                        end();
                    }

                    if (!StringUtils.isNullOrEmpty(style.getStroke())) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_STROKE_TOKEN, quote(style.getStroke()));
                        end();
                    }

                    if (style.getFontSize() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_FONT_SIZE_TOKEN, quote(style.getFontSize()));
                        end();
                    }

                    if (style.getBorder() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_BORDER_TOKEN, quote(style.getBorder()));
                        end();
                    }

                    if (style.getOpacity() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_OPACITY_TOKEN, quote(style.getOpacity()));
                        end();
                    }

                    if (style.getMetadata() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_METADATA_TOKEN, quote(style.getMetadata()));
                        end();
                    }

                    if (style.getDescription() != null) {
                        hasProperties = true;
                        start(ELEMENT_STYLE_DESCRIPTION_TOKEN, quote(style.getDescription()));
                        end();
                    }

                    if (!hasProperties) {
                        start("# empty style");
                        end();
                    }

                    end();
                });
                workspace.getViews().getConfiguration().getStyles().getRelationships().stream().sorted(Comparator.comparing(RelationshipStyle::getTag)).forEach(style -> {
                    boolean hasProperties = false;
                    start(RELATIONSHIP_STYLE_TOKEN, quote(style.getTag()));

                    if (style.getThickness() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_THICKNESS_TOKEN, quote(style.getThickness()));
                        end();
                    }

                    if (!StringUtils.isNullOrEmpty(style.getColor())) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_COLOR_TOKEN, quote(style.getColor()));
                        end();
                    }

                    if (style.getDashed() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_DASHED_TOKEN, quote(style.getDashed()));
                        end();
                    }

                    if (style.getRouting() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_ROUTING_TOKEN, quote(style.getRouting()));
                        end();
                    }

                    if (style.getFontSize() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_FONT_SIZE_TOKEN, quote(style.getFontSize()));
                        end();
                    }

                    if (style.getWidth() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_WIDTH_TOKEN, quote(style.getWidth()));
                        end();
                    }

                    if (style.getPosition() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_POSITION_TOKEN, quote(style.getPosition()));
                        end();
                    }

                    if (style.getOpacity() != null) {
                        hasProperties = true;
                        start(RELATIONSHIP_STYLE_OPACITY_TOKEN, quote(style.getOpacity()));
                        end();
                    }

                    if (!hasProperties) {
                        start("# empty style");
                        end();
                    }

                    end();
                });
                end();
            }

            if (hasThemes) {
                String[] themes = views.getConfiguration().getThemes();
                for (String theme : themes) {
                    start(THEMES_TOKEN, quote(theme));
                    end();
                }
            }

            if (hasBranding) {
                newline();
                start(BRANDING_TOKEN);
                String brandingLogo = workspace.getViews().getConfiguration().getBranding().getLogo();
                Font brandingFont = workspace.getViews().getConfiguration().getBranding().getFont();

                if (!StringUtils.isNullOrEmpty(brandingLogo)) {
                    start(BRANDING_LOGO_TOKEN, quote(brandingLogo));
                    end();
                }

                if (brandingFont != null) {
                    if (!StringUtils.isNullOrEmpty(brandingFont.getUrl())) {
                        start(BRANDING_FONT_TOKEN, quote(brandingFont.getName()), quote(brandingFont.getUrl()));
                        end();
                    } else if (!DEFAULT_FONT.equals(brandingFont.getName())){
                        start(BRANDING_FONT_TOKEN, quote(brandingFont.getName()));
                        end();
                    }
                }
                end();
            }

            newline();
            end();
        }

        newline();
        end();

        return buf.toString().replaceAll("\\{\\s*}", "");
    }

    private void format(final AutomaticLayout automaticLayout) {
        if (automaticLayout == null) {
            return;
        } else {
            String direction = "tb";
            switch (automaticLayout.getRankDirection()) {
                case TopBottom:
                    direction = "tb";
                    break;
                case BottomTop:
                    direction = "bt";
                    break;
                case LeftRight:
                    direction = "lr";
                    break;
                case RightLeft:
                    direction = "rl";
                    break;
            }
            start(AUTOLAYOUT_VIEW_TOKEN, direction, "" + automaticLayout.getRankSeparation(), "" + automaticLayout.getNodeSeparation());
            end();
        }
    }

    private void start(final String... tokens) {
        // remove empty tokens that appear at the end of the line
        List<String> list = Arrays.asList(tokens);
        boolean repeat = true;

        while (repeat) {
            if (list.get(list.size() - 1).equals("\"\"")) {
                list = list.subList(0, list.size() - 1);
                repeat = true;
            } else {
                repeat = false;
            }
        }

        format(true, list.toArray(new String[]{}));
        indent++;
    }

    private void end() {
        indent--;
        format(false, DslContext.CONTEXT_END_TOKEN);
    }

    private void newline() {
        buf.append(System.lineSeparator());
    }

    private String quote(final Object content) {
        String  s = "";

        if (content != null) {
            s = content.toString();
        }

        s = s.replaceAll("\\n", " ");
        s = s.replaceAll("\\\"", "\\\\\"");

        return String.format("\"%s\"", s);
    }

    private void format(final boolean startContext, final String... tokens) {
        indent();
        for (int i = 0; i < tokens.length; i++) {
            buf.append(tokens[i]);

            if (i < tokens.length - 1) {
                buf.append(" ");
            }
        }

        if (startContext) {
            buf.append(" ");
            buf.append(DslContext.CONTEXT_START_TOKEN);
        }

        newline();
    }

    private void indent() {
        for (int i = 0; i < indent * 4; i++) {
            buf.append(" ");
        }
    }

    private String tags(final Element element) {
        String tags = element.getTags();
        String defaultTags = "";

        if (element instanceof Person) {
            defaultTags = "Element,Person";
        } else if (element instanceof SoftwareSystem) {
            defaultTags = "Element,Software System";
        } else if (element instanceof Container) {
            defaultTags = "Element,Container";
        } else if (element instanceof Component) {
            defaultTags = "Element,Component";
        } else if (element instanceof DeploymentNode) {
            defaultTags = "Element,Deployment Node";
        } else if (element instanceof InfrastructureNode) {
            defaultTags = "Element,Infrastructure Node";
        } else if (element instanceof ContainerInstance) {
            defaultTags = "Container Instance";
        } else if (element instanceof SoftwareSystemInstance) {
            defaultTags = "Software System Instance";
        }

        tags = tags.substring(defaultTags.length());
        if (tags.startsWith(",")) {
            tags = tags.substring(1);
        }

        return tags;
    }

    private String tags(final Relationship relationship) {
        String tags = relationship.getTags();
        String defaultTags = "Relationship";

        if (tags.startsWith(defaultTags)) {
            tags = tags.substring(defaultTags.length());
            if (tags.startsWith(",")) {
                tags = tags.substring(1);
            }
        }

        return tags;
    }

    private void format(final Person person) {
        start(id(person), ASSIGNMENT_OPERATOR_TOKEN, PERSON_TOKEN, quote(person.getName()), quote(person.getDescription()), quote(tags(person)));
        formatModelItem(person);
        end();
    }

    private void formatModelItem(final ModelItem modelItem) {
        if (!StringUtils.isNullOrEmpty(modelItem.getUrl())) {
            start(URL_TOKEN, quote(modelItem.getUrl()));
            end();
        }

        if (!modelItem.getProperties().isEmpty()) {
            start(PROPERTIES_TOKEN);
            for (String name : modelItem.getProperties().keySet()) {
                start(quote(name), quote(modelItem.getProperties().get(name)));
                end();
            }
            end();
        }

        if (!modelItem.getPerspectives().isEmpty()) {
            start(PERSPECTIVES_TOKEN);
            for (Perspective perspective : modelItem.getPerspectives()) {
                start(quote(perspective.getName()), quote(perspective.getDescription()));
                end();
            }
            end();
        }

    }

    private String id(final ModelItem modelItem) {
        if (modelItem instanceof Person) {
            return id((Person)modelItem);
        } else  if (modelItem instanceof SoftwareSystem) {
            return id((SoftwareSystem)modelItem);
        } else  if (modelItem instanceof Container) {
            return id((Container)modelItem);
        } else  if (modelItem instanceof Component) {
            return id((Component)modelItem);
        } else  if (modelItem instanceof DeploymentNode) {
            return id((DeploymentNode)modelItem);
        } else  if (modelItem instanceof InfrastructureNode) {
            return id((InfrastructureNode)modelItem);
        } else  if (modelItem instanceof SoftwareSystemInstance) {
            return id((SoftwareSystemInstance)modelItem);
        } else  if (modelItem instanceof ContainerInstance) {
            return id((ContainerInstance)modelItem);
        }

        return modelItem.getId();
    }

    private String id(final ModelItem modelItem, final boolean hierarchical) {
        if (hierarchical) {
            if (modelItem instanceof Element) {
                Element element = (Element)modelItem;
                if (element.getParent() == null) {
                    if (element instanceof DeploymentNode) {
                        DeploymentNode dn = (DeploymentNode)element;
                        return filter(dn.getEnvironment()) + "." + id(dn);
                    } else {
                        return id(element);
                    }
                } else {
                    return id(element.getParent(), true) + "." + id(modelItem);
                }
            }
        }

        return id(modelItem);
    }

    private String id(final Person person) {
        return filter(person.getName());
    }

    private String id(final SoftwareSystem softwareSystem) {
        return filter(softwareSystem.getName());
    }

    private String id(final Container container) {
        return filter(container.getName());
    }

    private String id(final Component component) {
        return filter(component.getName());
    }

    private String id(final DeploymentNode deploymentNode) {
        return filter(deploymentNode.getName());
    }

    private String id(final InfrastructureNode infrastructureNode) {
        return filter(infrastructureNode.getName());
    }

    private String id(final SoftwareSystemInstance softwareSystemInstance) {
        return filter(softwareSystemInstance.getName()) + "_" + softwareSystemInstance.getInstanceId();
    }

    private String id(final ContainerInstance containerInstance) {
        return filter(containerInstance.getName()) + "_" + containerInstance.getInstanceId();
    }

    private String filter(final String s) {
        return s.replaceAll("\\W", "");
    }

    private void format(final SoftwareSystem softwareSystem) {
        start(id(softwareSystem), ASSIGNMENT_OPERATOR_TOKEN, SOFTWARE_SYSTEM_TOKEN, quote(softwareSystem.getName()), quote(softwareSystem.getDescription()), quote(tags(softwareSystem)));
        formatModelItem(softwareSystem);
        softwareSystem.getContainers().stream().sorted(Comparator.comparing(Container::getId)).forEach(this::format);
        end();
    }

    private void format(final Container container) {
        start(id(container), ASSIGNMENT_OPERATOR_TOKEN, CONTAINER_TOKEN, quote(container.getName()), quote(container.getDescription()), quote(container.getTechnology()), quote(tags(container)));
        formatModelItem(container);
        container.getComponents().stream().sorted(Comparator.comparing(Component::getId)).forEach(this::format);
        end();
    }

    private void format(final Component component) {
        start(id(component), ASSIGNMENT_OPERATOR_TOKEN, COMPONENT_TOKEN, quote(component.getName()), quote(component.getDescription()), quote(component.getTechnology()), quote(tags(component)));
        formatModelItem(component);
        end();
    }

    private void format(final DeploymentNode deploymentNode) {
        start(id(deploymentNode), ASSIGNMENT_OPERATOR_TOKEN, DEPLOYMENT_NODE_TOKEN, quote(deploymentNode.getName()), quote(deploymentNode.getDescription()), quote(deploymentNode.getTechnology()), quote(tags(deploymentNode)));
        formatModelItem(deploymentNode);

        deploymentNode.getInfrastructureNodes().stream().sorted(Comparator.comparing(InfrastructureNode::getId)).forEach(this::format);
        deploymentNode.getSoftwareSystemInstances().stream().sorted(Comparator.comparing(SoftwareSystemInstance::getId)).forEach(this::format);
        deploymentNode.getContainerInstances().stream().sorted(Comparator.comparing(ContainerInstance::getId)).forEach(this::format);

        deploymentNode.getChildren().stream().sorted(Comparator.comparing(DeploymentNode::getId)).forEach(this::format);

        end();
    }

    private void format(final InfrastructureNode infrastructureNode) {
        start(id(infrastructureNode), ASSIGNMENT_OPERATOR_TOKEN, INFRASTRUCTURE_NODE_TOKEN, quote(infrastructureNode.getName()), quote(infrastructureNode.getDescription()), quote(infrastructureNode.getTechnology()), quote(tags(infrastructureNode)));
        formatModelItem(infrastructureNode);
        end();
    }

    private void format(final SoftwareSystemInstance softwareSystemInstance) {
        start(id(softwareSystemInstance), ASSIGNMENT_OPERATOR_TOKEN, SOFTWARE_SYSTEM_INSTANCE_TOKEN, id(softwareSystemInstance.getSoftwareSystem(), true), quote(""), quote(tags(softwareSystemInstance)));
        formatModelItem(softwareSystemInstance);
        end();
    }

    private void format(final ContainerInstance containerInstance) {
        start(id(containerInstance), ASSIGNMENT_OPERATOR_TOKEN, CONTAINER_INSTANCE_TOKEN, id(containerInstance.getContainer(), true), quote(""), quote(tags(containerInstance)));
        formatModelItem(containerInstance);
        end();
    }

}
