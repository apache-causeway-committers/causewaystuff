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
package io.github.causewaystuff.treeview.metamodel.facets;

import java.util.Map;

import org.apache.causeway.applib.ViewModel;
import org.apache.causeway.applib.annotation.Programmatic;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.commons.collections.Can;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.treeview.applib.annotations.TreeSubNodes;

@UtilityClass
class _TreeSample {

    static interface SampleNode {
        String name();
    }

    record A(String name,
        @TreeSubNodes Can<B> childrenB,
        @TreeSubNodes Map<String, C> childrenC) implements SampleNode {
    }
    record B(String name,
        @TreeSubNodes Can<D> childrenD) implements SampleNode {
    }
    record C(String name,
        @TreeSubNodes Can<D> childrenD) implements SampleNode {
    }
    record D(String name) implements SampleNode {
    }

    A sampleA() {
        var ds = Can.of(new D("d1"), new D("d2"), new D("d3"));
        var cs = Can.of(new C("c1", ds), new C("c2", ds));
        var bs = Can.of(new B("b1", ds), new B("b2", ds));
        var a = new A("a", bs, cs.toMap(C::name));
        return a;
    }

    String nameOf(final Object node) {
        if(node instanceof SampleNode) {
            return ((SampleNode)node).name();
        }
        return "?";
    }

    public static class SampleNodeView implements ViewModel {

        @Programmatic
        final String memento;

        public SampleNodeView(final String memento) {
            this.memento = memento;
            this.name = "TODO";
        }

        @Override
        public String viewModelMemento() {
            return memento;
        }

        @Property @Getter
        final String name;

    }

}
