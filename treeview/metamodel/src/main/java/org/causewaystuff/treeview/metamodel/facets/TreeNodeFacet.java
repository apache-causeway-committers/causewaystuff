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

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

import org.apache.causeway.core.metamodel.facetapi.Facet;

import lombok.extern.log4j.Log4j2;

public interface TreeNodeFacet<T> extends Facet {
    Class<T> nodeType();
    Optional<Object> parentOf(final T node);
    int childCountOf(final T node);
    Stream<Object> childrenOf(final T node);

    default boolean isHandlingNodeType(final @Nullable Object node) {
        return node!=null
                ? nodeType().isAssignableFrom(node.getClass())
                : false;
    }
    
    default boolean isHandlingNodeTypeWarnIfNot(final @Nullable Object node) {
        if(node==null) return false;
        if(isHandlingNodeType(node)) return true;
        LogHelper.warnUnsupportedType(this, node.getClass());
        return false;
    }
    
    // -- HELPER
    
    @Log4j2
    static class LogHelper {
        static void warnUnsupportedType(TreeNodeFacet<?> treeNodeFacet, Class<?> actualNodeType) {
            log.warn("TreeNodeFacet for type {} does not handle ", 
                    treeNodeFacet.nodeType().getName(),
                    actualNodeType.getName());
        }
    }
}
