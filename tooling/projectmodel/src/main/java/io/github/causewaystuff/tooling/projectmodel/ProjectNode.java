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
package io.github.causewaystuff.tooling.projectmodel;

import java.io.File;
import java.util.TreeSet;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@Data @Builder
public class ProjectNode implements Comparable<ProjectNode> {

    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final ProjectNode parent;

    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final TreeSet<ProjectNode> children = new TreeSet<ProjectNode>(
            (a,b)->a.getName().compareTo(b.getName()));

    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final TreeSet<Dependency> dependencies = new TreeSet<Dependency>();

    private final ArtifactCoordinates artifactCoordinates;
    private final String name;
    private final String description;
    private final File projectDirectory;

    public void depthFirst(final @NonNull ProjectVisitor projectVisitor) {
        projectVisitor.accept(this);
        for(var child : getChildren()){
            child.depthFirst(projectVisitor);
        }
    }

    public boolean contains(final @NonNull ProjectNode other) {
        for(var child : getChildren()){
            if(child.containsOrEquals(other)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsOrEquals(final @NonNull ProjectNode other) {
        if(this.getArtifactCoordinates().equals(other.getArtifactCoordinates())) {
            return true;
        }
        for(var child : getChildren()){
            if(child.containsOrEquals(other)) {
                return true;
            }
        }
        return false;
    }

    // -- COMPARATOR

    @Override
    public int compareTo(final ProjectNode other) {
        if(this.contains(other)) {
            return -1;
        }
        if(other.contains(this)) {
            return 1;
        }
        return this.getArtifactCoordinates().compareTo(other.getArtifactCoordinates());
    }

}
