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
package guru.nidi.codeassert.model;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.emptyList;

import guru.nidi.codeassert.config.LocationMatcher;
import guru.nidi.codeassert.util.CountSet;

/**
 * The <code>JavaClass</code> class represents a Java
 * class or interface.
 */
public class CodeClass extends UsingElement<CodeClass> {
    private final String name;
    private final CodePackage pack;
    private final CountSet<CodePackage> usedPackages;
    private final CountSet<CodeClass> usedClasses;
    private final Set<CodeClass> annotations;
    final List<MemberInfo> fields = new ArrayList<>();
    final List<MemberInfo> methods = new ArrayList<>();
    final SortedSet<String> interfaces = new TreeSet<>();
    String superClass;
    String sourceFile;
    int codeSize;
    int totalSize;
    int flags;
    int sourceSize;
    int codeLines;
    int commentLines;
    int emptyLines;
    int totalLines;

    public CodeClass(final String fullName) {
        this(fullName, new CodePackage(fullName.substring(0, fullName.lastIndexOf('.'))));
    }

    CodeClass(final String name, final CodePackage pack) {
        this.name = name;
        this.pack = pack;
        usedPackages = new CountSet<>();
        usedClasses = new CountSet<>();
        annotations = new HashSet<>();
        sourceFile = "Unknown";
    }

    public boolean isParsed() {
        return superClass != null;
    }

    public List<MemberInfo> getMembers() {
        final List<MemberInfo> members = new ArrayList<>();
        members.addAll(methods);
        members.addAll(fields);
        return members;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSimpleName() {
        return name.substring(pack.getName().length() + 1);
    }

    public CodePackage getPackage() {
        return pack;
    }

    public String getSuperClass() {
        return superClass;
    }

    public SortedSet<String> getInterfaces() {
        return Collections.unmodifiableSortedSet(interfaces);
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public Set<CodeClass> getAnnotations() {
        return annotations;
    }

    public List<MemberInfo> getFields() {
        return fields;
    }

    public List<MemberInfo> getMethods() {
        return methods;
    }

    public int getCodeSize() {
        return codeSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public boolean isConcrete() {
        return !Modifier.isAbstract(flags) && !Modifier.isInterface(flags);
    }

    public int getSourceSize() {
        return sourceSize;
    }

    public int getCodeLines() {
        return codeLines;
    }

    public int getCommentLines() {
        return commentLines;
    }

    public int getEmptyLines() {
        return emptyLines;
    }

    public int getTotalLines() {
        return totalLines;
    }

    @Override
    public CodeClass self() {
        return this;
    }

    public Collection<CodePackage> usedForeignPackages() {
        final Set<CodePackage> res = new HashSet<>(usedPackages());
        res.remove(pack);
        return res;
    }

    public Collection<CodePackage> usedPackages() {
        return usedPackages.asSet();
    }

    public Map<CodePackage, Integer> usedPackageCounts() {
        return usedPackages.asMap();
    }

    public Collection<CodeClass> usedClasses() {
        return usedClasses.asSet();
    }

    public Map<CodeClass, Integer> usedClassCounts() {
        return usedClasses.asMap();
    }

    public boolean uses(final CodePackage pack) {
        return usedPackages.contains(pack);
    }

    @Override
    public Collection<CodeClass> uses() {
        return usedClasses();
    }

    @Override
    public String getPackageName() {
        return pack.getName();
    }

    @Override
    public Collection<String> usedVia(final UsingElement<CodeClass> other) {
        return emptyList();
    }

    @Override
    public boolean isMatchedBy(final LocationMatcher matcher) {
        return matcher.matchesClass(name);
    }

    void addImport(final String type, final Model model) {
        if (!name.equals(type)) {
            final CodeClass clazz = model.getOrCreateClass(type);
            if (clazz != null) {
                final String packName = model.packageOf(type);
                final CodePackage p = model.getOrCreatePackage(packName);
                usedPackages.add(p);
                pack.addEfferent(p);
                usedClasses.add(clazz);
            }
        }
    }

    void addAnnotation(final String type, final Model model, final Collection<CodeClass> annotations) {
        final CodeClass clazz = model.getOrCreateClass(type);
        if (clazz != null) {
            addImport(type, model);
            annotations.add(clazz);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof final CodeClass otherClass)
            return otherClass.getName().equals(getName()) && otherClass.getPackage().equals(getPackage());
        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode() * 31 + getPackage().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
