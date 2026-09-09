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
package io.github.causewaystuff.tooling.projectexplorer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.JsonUtils;
import org.apache.causeway.commons.io.YamlUtils;

import io.github.causewaystuff.tooling.codeassert.config.AnalyzerConfig;
import io.github.causewaystuff.tooling.codeassert.config.Language;
import io.github.causewaystuff.tooling.codeassert.model.CodeClass;
import io.github.causewaystuff.tooling.codeassert.model.MemberInfo;
import io.github.causewaystuff.tooling.codeassert.model.Model;
import io.github.causewaystuff.tooling.javamodel.AnalyzerConfigFactory;
import io.github.causewaystuff.tooling.projectmodel.ProjectNode;
import io.github.causewaystuff.tooling.projectmodel.ProjectNodeFactory;
import io.github.causewaystuff.tooling.projectmodel.ProjectVisitor;

public record ProjectExplorer(
        Map<String, ResolvedProject> projectByName,
        Map<String, ResolvedClass> classByQualifiedName) {

    public record ResolvedProject(
            ProjectDescriptor projDescriptor,
            SortedSet<ResolvedClass> classes) {

        record Dto(ProjectDescriptor projDescriptor,
                List<ResolvedClass.Dto> classes) {
        }

        public String name() { return projDescriptor.projName(); }

        public Optional<Path> sourcePath(final CodeClass codeClass) {
            var sourceFile = codeClass.getSourceFile();
            return StringUtils.hasText(sourceFile)
                    && !"Unknown".equals(sourceFile)
                ? Optional.of(projDescriptor.projPath()
                        .resolve("src")
                        .resolve("main")
                        .resolve("java")
                        .resolve(Path.of(codeClass.getPackageName().replace('.', '/')))
                        .resolve(codeClass.getSourceFile()))
                    .filter(Files::exists)
                : Optional.empty();
        }
        public Dto toDto() {
            return new Dto(projDescriptor, classes.stream().map(ResolvedClass::toDto).toList());
        }
        public String toJson() { return JsonUtils.toStringUtf8(toDto()); }
        public String toYaml() { return YamlUtils.toStringUtf8(toDto()); }
    }

    public record ResolvedClass(
            String qualifiedName,
            Optional<ProjectDescriptor> projectDescriptor,
            CodeClass codeClass) implements Comparable<ResolvedClass> {

        record Dto(String qualifiedName,
                String superclass,
                SortedSet<String> interfaces,
                List<FieldDto> fields,
                List<MethodDto> methods) {
        }
        record FieldDto(String name, Collection<String> referencedClasses) {
            public FieldDto(final MemberInfo info) {
                this(info.getName(), info.getReferencedClasses());
            }
        }
        record MethodDto(String name, Collection<String> referencedClasses) {
            public MethodDto(final MemberInfo info) {
                this(info.getName(), info.getReferencedClasses());
            }
        }
        public String simpleName() {
            return codeClass.getSimpleName();
        }
        public boolean isDirectSubTypeOf(final ResolvedClass resolvedClass) {
            return Objects.equals(codeClass.getSuperClass(), resolvedClass.qualifiedName)
                    || codeClass.getInterfaces().contains(resolvedClass.qualifiedName);
        }
        public Optional<Path> sourcePath(final ProjectExplorer explorer) {
            return projectDescriptor
                    .map(ProjectDescriptor::projName)
                    .map(explorer.projectByName::get)
                    .flatMap(proj->proj.sourcePath(codeClass));
        }
        public Dto toDto() {
            return new Dto(qualifiedName,
                    codeClass.getSuperClass(),
                    codeClass.getInterfaces(),
                    codeClass.getFields().stream().map(FieldDto::new).toList(),
                    codeClass.getMethods().stream().map(MethodDto::new).toList());
        }
        public String toJson() { return JsonUtils.toStringUtf8(toDto()); }
        public String toYaml() { return YamlUtils.toStringUtf8(toDto()); }
        @Override public int compareTo(final ResolvedClass o) {
            return _Strings.compareNullsFirst(this.qualifiedName, o.qualifiedName);
        }
    }

    public static ProjectExplorer from(final Collection<ProjectDescriptor> projDescriptors) {
        var projectTrees = _NullSafe.stream(projDescriptors)
            .map(ProjectTree::new)
            .toList();

        var projects = projectTrees.stream()
            .flatMap(ProjectTree::streamDescriptors)
            .map(ProjectBuilder::new)
            .map(ProjectBuilder::build)
            .toList();

        var projectByName = projects.stream()
            .collect(Collectors.toMap(ResolvedProject::name, UnaryOperator.identity(), (a, b)->a, TreeMap::new));

        var classByQualifiedName = projects.stream()
            .flatMap(proj->proj.classes().stream())
            .collect(Collectors.toMap(ResolvedClass::qualifiedName, UnaryOperator.identity(), (a, b)->a, TreeMap::new));

        return new ProjectExplorer(projectByName, classByQualifiedName);
    }

    public SortedSet<ResolvedClass> directSubTypesOf(final ResolvedClass resolvedClass) {
        return classByQualifiedName.values().stream()
            .filter(it->it.isDirectSubTypeOf(resolvedClass))
            .collect(Collectors.toCollection(TreeSet::new));
    }
    public SortedSet<ResolvedClass> subTypesOf(final ResolvedClass resolvedClass) {
        var result = new TreeSet<ResolvedClass>();
        for(
                var next = directSubTypesOf(resolvedClass);
                !next.isEmpty();
                next = next.stream()
                        .flatMap(it->directSubTypesOf(it).stream())
                        .collect(Collectors.toCollection(TreeSet::new))) {
            result.addAll(next);
        }
        return result;
    }

    public Optional<ResolvedClass> lookupClassForQualifiedName(final @Nullable String qualifiedName) {
        return Optional.ofNullable(classByQualifiedName.get(qualifiedName));
    }

    public Optional<Path> lookupSourceForClass(final @Nullable ResolvedClass resolvedClass) {
        return Optional.ofNullable(resolvedClass)
                .flatMap(cls->cls.sourcePath(this));
    }

    public Optional<Path> lookupSourceForQualifiedName(final @Nullable String qualifiedName) {
        return lookupClassForQualifiedName(qualifiedName)
                .flatMap(this::lookupSourceForClass);
    }

    // -- HELPER

    private record ProjectTree(
            ProjectDescriptor rootDescriptor,
            ProjectNode root) {
        ProjectTree(
                final ProjectDescriptor rootDescriptor) {
            this(rootDescriptor, ProjectNodeFactory.maven(rootDescriptor.projPath().toFile()));
        }
        Stream<ProjectDescriptor> streamDescriptors() {
            return Stream.concat(Stream.of(rootDescriptor), subProjectDescriptors().stream());
        }
        List<ProjectDescriptor> subProjectDescriptors() {
            if(!rootDescriptor.recure())
                return List.of();
            var subProjectDescriptors = new ArrayList<ProjectDescriptor>();
            root.depthFirst((ProjectVisitor) projModel -> {
                if(projModel == root)
                    return;
                var sub = new ProjectDescriptor(
                        projModel.getArtifactCoordinates().getArtifactId(),
                        rootDescriptor.packageFilter(),
                        projModel.getProjectDirectory().toPath(),
                        false);
                subProjectDescriptors.add(sub);
            });
            return Collections.unmodifiableList(subProjectDescriptors);
        }
    }

    private record ProjectBuilder(
            ProjectDescriptor projDescriptor,
            AnalyzerConfig analyzerConfig) {
        ProjectBuilder(
                final ProjectDescriptor projDescriptor) {
            this(projDescriptor, analyzerConfig(projDescriptor));
        }
        ResolvedProject build() {
            var classes = Model.from(analyzerConfig.getClasses()).read().getClasses()
                .stream()
                .filter(codeClass->codeClass.getPackageName().startsWith(projDescriptor.packageFilter()))
                .map(codeClass->new ClassBuilder(
                      Optional.of(this),
                      codeClass))
                .map(ClassBuilder::build)
                .collect(Collectors.toCollection(TreeSet::new));
            return new ResolvedProject(projDescriptor, classes);
        }
        private static AnalyzerConfig analyzerConfig(final ProjectDescriptor projDescriptor) {
            return AnalyzerConfigFactory.maven(projDescriptor.projPath().toFile(), Language.JAVA).main();
        }
    }

    private final static class ClassBuilder {
        final Optional<ProjectBuilder> projectBuilder;
        final CodeClass codeClass;
        ResolvedClass resolvedClass;
        ClassBuilder(
                final Optional<ProjectBuilder> projectBuilder,
                final CodeClass codeClass) {
            this.projectBuilder = projectBuilder;
            this.codeClass = codeClass;
        }
        // idempotent
        ResolvedClass build() {
            if(resolvedClass==null) {
                resolvedClass = new ResolvedClass(codeClass.getName(), projectBuilder.map(ProjectBuilder::projDescriptor), codeClass);
            }
            return resolvedClass;
        }
    }

}
