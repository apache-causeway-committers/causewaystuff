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
package io.github.causewaystuff.treeview.applib.factories;

import org.apache.causeway.applib.graph.tree.TreeAdapter;
import org.apache.causeway.applib.graph.tree.TreeNode;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.commons.internal.base._Casts;
import org.apache.causeway.commons.internal.context._Context;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import io.github.causewaystuff.commons.base.util.RuntimeUtils;

@UtilityClass
public class TreeNodeFactory {

    public TreeNode<Object> wrap(final Object treeNode) {
        return wrap(treeNode, RuntimeUtils.getFactoryService());
    }
    public TreeNode<Object> wrap(final Object treeNode, final FactoryService factoryService) {
        return TreeNode.root(treeNode, defaultTreeAdapter(), factoryService);
    }

    /**
     * Does not guarantee at compile time, that the tree is actually made up of nodes that are instances of {@code commonNodeType}.
     * In the sad case may result in {@link ClassCastException}(s).
     */
    public <T, R extends T> TreeNode<T> wrap(final Class<T> commonNodeType, final R treeNode) {
        return wrap(commonNodeType, treeNode, RuntimeUtils.getFactoryService());
    }
    /**
     * Does not guarantee at compile time, that the tree is actually made up of nodes that are instances of {@code commonNodeType}.
     * In the sad case may result in {@link ClassCastException}(s).
     */
    public <T, R extends T> TreeNode<T> wrap(final Class<T> commonNodeType, final R treeNode, final FactoryService factoryService) {
        return TreeNode.root(treeNode, _Casts.uncheckedCast(defaultTreeAdapter()), factoryService);
    }

    // -- HELPER

    @SneakyThrows //TODO if class not found exception: some message that the metamodel module is missing
    @SuppressWarnings("unchecked")
    private Class<? extends TreeAdapter<Object>> defaultTreeAdapter() {
        return (Class<? extends TreeAdapter<Object>>)
                _Context.loadClass("io.github.causewaystuff.treeview.metamodel.treeadapter.ObjectTreeAdapter");
    }

}
