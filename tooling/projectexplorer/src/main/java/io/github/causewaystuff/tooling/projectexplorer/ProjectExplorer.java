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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.JsonUtils;
import org.apache.causeway.commons.io.YamlUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	public enum SourceType {
		UNKNOWN,
		MAIN,
		TEST,
	}

    public record ResolvedProject(
            ProjectDescriptor rootDescriptor,
            ProjectDescriptor projDescriptor,
            SortedSet<ResolvedClass> classes) {

        record Dto(ProjectDescriptor projDescriptor,
                List<ResolvedClass.Dto> classes) {
        }

        public String rootName() { return rootDescriptor.projName(); }
        public String name() { return projDescriptor.projName(); }
        public boolean isRoot() { return projDescriptor.equals(rootDescriptor); }

        public Optional<Path> sourcePath(final CodeClass codeClass, final SourceType sourceType) {
            var sourceFile = codeClass.getSourceFile();
            return StringUtils.hasText(sourceFile)
                    && !"Unknown".equals(sourceFile)
                ? Optional.of(projDescriptor.projPath()
                        .resolve("src")
                        .resolve(sourceType.name().toLowerCase())
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

        /**
         * @return whether this is a sub-project of given root-project or equals the root-project
         */
        public boolean isSubProjectOf(final ResolvedProject rootProject) {
            Assert.isTrue(rootProject.isRoot(), ()->"not a root project %s".formatted(rootProject.projDescriptor()));
            return rootDescriptor.equals(rootProject.rootDescriptor());
        }
    }

    public record ResolvedClass(
            String qualifiedName,
            Optional<ProjectDescriptor> projectDescriptor,
            SourceType sourceType,
            CodeClass codeClass) implements Comparable<ResolvedClass> {

        public record Dto(String qualifiedName,
                String superclass,
                SortedSet<String> interfaces,
                List<FieldDto> fields,
                List<MethodDto> methods) {
        }
        record FieldDto(String name, String referencedClass) {
            public FieldDto(final MemberInfo info) {
                this(info.getName(), info.getReferencedClasses().iterator().next());
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
        public boolean isDirectlyReferencing(final ResolvedClass resolvedClass) {
            return isDirectSubTypeOf(resolvedClass)
                || codeClass.getMembers().stream()
                    .map(MemberInfo::getReferencedClasses)
                    .flatMap(Set::stream)
                    .anyMatch(resolvedClass.qualifiedName::equals);
        }
        public Optional<Path> sourcePath(final ProjectExplorer explorer) {
            return projectDescriptor
                    .map(ProjectDescriptor::projName)
                    .map(explorer.projectByName::get)
                    .flatMap(proj->proj.sourcePath(codeClass, sourceType));
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
            .flatMap(ProjectTree::streamProjectBuilders)
            .map(ProjectBuilder::build)
            .toList();

        var projectByName = projects.stream()
            .collect(Collectors.toMap(ResolvedProject::name, UnaryOperator.identity(), (a, b)->a, TreeMap::new));

        var classByQualifiedName = projects.stream()
            .flatMap(proj->proj.classes().stream())
            .collect(Collectors.toMap(ResolvedClass::qualifiedName, UnaryOperator.identity(), (a, b)->a, TreeMap::new));

        return new ProjectExplorer(projectByName, classByQualifiedName);
    }

    /** All direct sub-types (extending/implementing classes and interfaces) of the given class, across all indexed projects. */
    public SortedSet<ResolvedClass> directSubTypesOf(final ResolvedClass resolvedClass) {
        return classByQualifiedName.values().stream()
            .filter(it->it.isDirectSubTypeOf(resolvedClass))
            .collect(Collectors.toCollection(TreeSet::new));
    }
    /** All transitive sub-types of the given class/interface, across all indexed projects. */
    public SortedSet<ResolvedClass> subTypesOf(final ResolvedClass resolvedClass) {
        var result = new TreeSet<ResolvedClass>();
        for(
                var next = directSubTypesOf(resolvedClass);
                !next.isEmpty();
                next = next.stream()
                        .map(this::directSubTypesOf)
                        .flatMap(SortedSet::stream)
                        .collect(Collectors.toCollection(TreeSet::new))) {
            result.addAll(next);
        }
        return result;
    }

    /** All classes that directly reference the given class/interface, across all indexed projects. */
    public SortedSet<ResolvedClass> directReferencersOf(final ResolvedClass resolvedClass) {
        return classByQualifiedName.values().stream()
                .filter(it->it.isDirectlyReferencing(resolvedClass))
                .collect(Collectors.toCollection(TreeSet::new));
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

    public Optional<ResolvedProject> lookupProjectForClass(final @Nullable ResolvedClass resolvedClass) {
        return Optional.ofNullable(resolvedClass)
                .flatMap(ResolvedClass::projectDescriptor)
                .map(ProjectDescriptor::projName)
                .map(projectByName::get);
    }

    public Optional<ResolvedProject> lookupProjectForQualifiedName(final @Nullable String qualifiedName) {
        return lookupClassForQualifiedName(qualifiedName)
                .flatMap(this::lookupProjectForClass);
    }

    // -- HELPER

    private record ProjectTree(
            ProjectDescriptor rootDescriptor,
            ProjectNode root) {
        ProjectTree(
                final ProjectDescriptor rootDescriptor) {
            this(rootDescriptor, ProjectNodeFactory.maven(rootDescriptor.projPath().toFile()));
        }
        Stream<ProjectBuilder> streamProjectBuilders() {
            return Stream.concat(Stream.of(rootDescriptor), subProjectDescriptors().stream())
                    .map(desc->new ProjectBuilder(rootDescriptor, desc, rootDescriptor.includeTests()));
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
                        rootDescriptor.includeTests(),
                        false);
                subProjectDescriptors.add(sub);
            });
            return Collections.unmodifiableList(subProjectDescriptors);
        }
    }

    private record ProjectBuilder(
            ProjectDescriptor rootDescriptor,
            ProjectDescriptor projDescriptor,
            boolean includeTests) {
        ResolvedProject build() {
        	var analyzerConfig = AnalyzerConfigFactory.maven(projDescriptor.projPath().toFile(), Language.JAVA).main();
            var mainClasses = Model.from(analyzerConfig.getClasses()).read().getClasses()
                .stream()
                .filter(codeClass->codeClass.getPackageName().startsWith(projDescriptor.packageFilter()))
                .map(codeClass->new ClassBuilder(
                      Optional.of(this),
                      SourceType.MAIN,
                      codeClass))
                .map(ClassBuilder::build)
                .collect(Collectors.toCollection(TreeSet::new));

            SortedSet<ResolvedClass> classes = mainClasses;

            if(includeTests) {
            	analyzerConfig = AnalyzerConfigFactory.mavenTest(projDescriptor.projPath().toFile(), Language.JAVA).main();
	            Model.from(analyzerConfig.getClasses()).read().getClasses()
	                .stream()
	                .filter(codeClass->codeClass.getPackageName().startsWith(projDescriptor.packageFilter()))
	                .map(codeClass->new ClassBuilder(
	                      Optional.of(this),
	                      SourceType.TEST,
	                      codeClass))
	                .map(ClassBuilder::build)
	                .forEach(classes::add);
            }

            return new ResolvedProject(rootDescriptor, projDescriptor, classes);
        }
    }

    private final static class ClassBuilder {
        final Optional<ProjectBuilder> projectBuilder;
        final SourceType sourceType;
        final CodeClass codeClass;
        ResolvedClass resolvedClass;
        ClassBuilder(
                final Optional<ProjectBuilder> projectBuilder,
                final SourceType sourceType,
                final CodeClass codeClass) {
            this.projectBuilder = projectBuilder;
            this.sourceType = sourceType;
            this.codeClass = codeClass;
        }
        // idempotent
        ResolvedClass build() {
            if(resolvedClass==null) {
                resolvedClass = new ResolvedClass(
                        codeClass.getName(), projectBuilder.map(ProjectBuilder::projDescriptor), sourceType, codeClass);
            }
            return resolvedClass;
        }
    }

}
