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
package org.causewaystuff.treeview.metamodel.facets;

import org.causewaystuff.treeview.applib.annotations.TreeSubNodes;

import org.apache.causeway.commons.collections.Can;

import lombok.experimental.UtilityClass;

@UtilityClass
class _TreeSample {

    record A(String name,
        @TreeSubNodes Can<B> childrenB,
        @TreeSubNodes Can<C> childrenC) {
    }
    record B(String name,
        @TreeSubNodes Can<D> childrenD) {
    }
    record C(String name,
        @TreeSubNodes Can<D> childrenD) {
    }
    record D(String name) {
    }
    
    A sampleA() {
        var ds = Can.of(new D("d1"), new D("d2"), new D("d3"));
        var cs = Can.of(new C("c1", ds), new C("c2", ds));
        var bs = Can.of(new B("b1", ds), new B("b2", ds));
        var a = new A("a", bs, cs);
        return a;
    }
    
    String nameOf(Object node) {
        if(node instanceof A) {
            return ((A)node).name();
        } else if(node instanceof B) {
            return ((B)node).name();
        } else if(node instanceof C) {
            return ((C)node).name();
        } else if(node instanceof D) {
            return ((D)node).name();
        }
        return "?";
    }

}
