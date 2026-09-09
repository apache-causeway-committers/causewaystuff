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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.internal.collections._Maps;
import org.apache.causeway.commons.io.JsonUtils;
import org.apache.causeway.commons.io.YamlUtils;

import lombok.Getter;
import lombok.experimental.Accessors;

import io.github.causewaystuff.tooling.codeassert.config.AnalyzerConfig;
import io.github.causewaystuff.tooling.codeassert.config.Language;
import io.github.causewaystuff.tooling.codeassert.model.CodeClass;
import io.github.causewaystuff.tooling.codeassert.model.CodePackage;
import io.github.causewaystuff.tooling.codeassert.model.MemberInfo;
import io.github.causewaystuff.tooling.codeassert.model.Model;
import io.github.causewaystuff.tooling.javamodel.AnalyzerConfigFactory;

public record ProjectExplorer(
        Map<String, ResolvedProject> projectByName,
        Map<String, ResolvedClass> classByQualifiedName,
        Model model) {

    public record ResolvedProject(
            ProjectDescriptor projDescriptor,
            SortedSet<ResolvedClass> classes) {

        record Dto(ProjectDescriptor projDescriptor,
                List<ResolvedClass.Dto> classes) {
        }

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

    private record ProjectBuilder(
            ProjectDescriptor projDescriptor,
            AnalyzerConfig analyzerConfig,
            List<ClassBuilder> classBuilders) {
        ProjectBuilder(
                final ProjectDescriptor projDescriptor,
                final AnalyzerConfig analyzerConfig) {
            this(projDescriptor, analyzerConfig, new ArrayList<>());
        }
        void addClass(final ClassBuilder classBuilder) {
            classBuilders.add(classBuilder);
        }
        ResolvedProject build() {
            return new ResolvedProject(projDescriptor, classBuilders.stream()
                    .map(ClassBuilder::build)
                    .collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    private final static class ClassBuilder {
        @Getter @Accessors(fluent = true) final String qualifiedName;
        final Optional<ProjectBuilder> projectBuilder;
        final CodeClass codeClass;
        ResolvedClass resolvedClass;
        ClassBuilder(
                final String qualifiedName,
                final Optional<ProjectBuilder> projectBuilder,
                final CodeClass codeClass) {
            this.qualifiedName = qualifiedName;
            this.projectBuilder = projectBuilder;
            this.codeClass = codeClass;
            projectBuilder.ifPresent(proj->proj.addClass(this));
        }
        // idempotent
        ResolvedClass build() {
            if(resolvedClass==null) {
                resolvedClass = new ResolvedClass(qualifiedName, projectBuilder.map(ProjectBuilder::projDescriptor), codeClass);
            }
            return resolvedClass;
        }
    }

    public static ProjectExplorer from(final Collection<ProjectDescriptor> projDescriptors) {
        var projectBuilderByName = _NullSafe.stream(projDescriptors)
            .map(projDesc->new ProjectBuilder(projDesc, AnalyzerConfigFactory.maven(projDesc.projPath().toFile(), Language.JAVA).main()))
            .collect(Collectors.toMap(it->it.projDescriptor().projName(), UnaryOperator.identity()));

        var allClassFiles = projectBuilderByName.values().stream()
            .map(ProjectBuilder::analyzerConfig)
            .map(AnalyzerConfig::getClasses)
            .flatMap(List::stream)
            .toList();
        var model = Model.from(allClassFiles).read();

        var classBuilderByQualifiedName = model.getClasses().stream()
            .map(codeClass->new ClassBuilder(
                    codeClass.getName(),
                    lookupMatchingProjectDescriptor(projDescriptors, codeClass.getPackage())
                        .map(ProjectDescriptor::projName)
                        .map(projectBuilderByName::get),
                    codeClass))
            .collect(Collectors.toMap(ClassBuilder::qualifiedName, UnaryOperator.identity()));

        var projectByName = _Maps.mapValues(projectBuilderByName, TreeMap::new, ProjectBuilder::build);
        var classByQualifiedName = _Maps.mapValues(classBuilderByQualifiedName, TreeMap::new, ClassBuilder::build);

        return new ProjectExplorer(projectByName, classByQualifiedName, model);
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

    // -- HELPER

    private static Optional<ProjectDescriptor> lookupMatchingProjectDescriptor(
            final Collection<ProjectDescriptor> projectDescriptors,
            final CodePackage codePackage) {
        final var pkgName = codePackage.getPackageName();
        return projectDescriptors.stream()
            .filter(desc->pkgName.startsWith(desc.packageFilter()))
            .findFirst();
    }

}
