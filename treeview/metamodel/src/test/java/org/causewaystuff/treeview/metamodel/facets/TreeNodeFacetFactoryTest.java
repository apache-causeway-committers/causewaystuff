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

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.causewaystuff.treeview.applib.annotations.TreeSubNodes;
import org.causewaystuff.treeview.applib.factories.TreeNodeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.causeway.applib.graph.tree.TreeNode;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel._testing.MetaModelContext_forTesting;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.facets.ObjectTypeFacetFactory.ProcessObjectTypeContext;

class TreeNodeFacetFactoryTest
extends FacetFactoryTestAbstract {

    TreeNodeFacetFactory facetFactory;

    @BeforeEach
    void setUp() {
        var mmc = MetaModelContext_forTesting.buildDefault();
        assertNotNull(MetaModelContext.instanceNullable());
        facetFactory = new TreeNodeFacetFactory(mmc);
    }

    @AfterEach
    protected void tearDown() {
        facetFactory = null;
    }

    // -- SCENARIOS

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

    @Test
    void test() {

        //TODO perhaps add also parent lookup support

        objectScenario(A.class, (processClassContext, facetHolder)->{
            facetFactory.process(new ProcessObjectTypeContext(processClassContext.getCls(), facetHolder));
            assertNotNull(facetHolder.getFacet(TreeNodeFacet.class));
        });
        objectScenario(B.class, (processClassContext, facetHolder)->{
            facetFactory.process(new ProcessObjectTypeContext(processClassContext.getCls(), facetHolder));
            assertNotNull(facetHolder.getFacet(TreeNodeFacet.class));
        });
        objectScenario(C.class, (processClassContext, facetHolder)->{
            facetFactory.process(new ProcessObjectTypeContext(processClassContext.getCls(), facetHolder));
            assertNotNull(facetHolder.getFacet(TreeNodeFacet.class));
        });
        objectScenario(D.class, (processClassContext, facetHolder)->{
            facetFactory.process(new ProcessObjectTypeContext(processClassContext.getCls(), facetHolder));
            assertNull(facetHolder.getFacet(TreeNodeFacet.class));
        });

        //FIXME this cannot work, requires a valid ObjectSpecifications

        // instantiate a tree, that we later traverse
        var ds = Can.of(new D("d1"), new D("d2"), new D("d3"));
        var cs = Can.of(new C("c1", ds), new C("c2", ds));
        var bs = Can.of(new B("b1", ds), new B("b2", ds));
        var a = new A("a", bs, cs);

        // traverse the tree
        var tree = TreeNodeFactory.wrap(a);
        var nodeNames = new ArrayList<String>();
        tree.iteratorDepthFirst().forEachRemaining((TreeNode<Object> treeNode)->{
            var node = treeNode.getValue();
            if(node instanceof A) {
                nodeNames.add(((A)node).name());
            } else if(node instanceof B) {
                nodeNames.add(((B)node).name());
            } else if(node instanceof C) {
                nodeNames.add(((C)node).name());
            } else if(node instanceof D) {
                nodeNames.add(((D)node).name());
            }
        });

        //TODO fix introspection to include child node types
        assertEquals("a", nodeNames.stream().collect(Collectors.joining(", ")));

    }
}
