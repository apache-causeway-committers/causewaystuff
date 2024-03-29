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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.causewaystuff.treeview.applib.annotations.TreeSubNodes;

import org.springframework.stereotype.Component;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.functional.Try;
import org.apache.causeway.commons.internal.base._NullSafe;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.facetapi.FacetAbstract;
import org.apache.causeway.core.metamodel.facetapi.FacetHolder;
import org.apache.causeway.core.metamodel.facetapi.FeatureType;
import org.apache.causeway.core.metamodel.facetapi.MetaModelRefiner;
import org.apache.causeway.core.metamodel.facets.FacetFactoryAbstract;
import org.apache.causeway.core.metamodel.facets.ObjectTypeFacetFactory;
import org.apache.causeway.core.metamodel.progmodel.ProgrammingModel;
import org.apache.causeway.core.metamodel.progmodel.ProgrammingModel.FacetProcessingOrder;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class TreeNodeFacetFactory
extends FacetFactoryAbstract
implements
    MetaModelRefiner,
    ObjectTypeFacetFactory {

    private final MethodHandles.Lookup lookup;

    @Inject
    public TreeNodeFacetFactory(final MetaModelContext metaModelContext) {
        super(metaModelContext, FeatureType.OBJECTS_ONLY);
        this.lookup = MethodHandles.lookup();
    }

    @Override
    public void refineProgrammingModel(final ProgrammingModel programmingModel) {
        programmingModel.addFactory(FacetProcessingOrder.Z2_AFTER_FINALLY, this);
    }

    @Override
    public void process(final ProcessObjectTypeContext processClassContext) {
        var cls = processClassContext.getCls();
        if(!cls.isRecord()) {
            return; //TODO yet only record types are supported
        }

        //TODO also introspect types of the children
        var treeSubNodesMethodHandlers = _NullSafe.stream(cls.getRecordComponents())
            .filter(rc->rc.isAnnotationPresent(TreeSubNodes.class))
            .map(rc->{
                MethodHandle targetMh = Try.call(()->
                        lookup.findVirtual(cls, rc.getName(), MethodType.methodType(rc.getType())))
                        .valueAsNonNullElseFail();
                return targetMh;
            })
            .collect(Can.toCan());

        //TODO also lookup @TreeSuperNode and find the method-handle
        var treeSuperNodeMethodHandler = Optional.<MethodHandle>empty();

        if(treeSubNodesMethodHandlers.isEmpty()) return;

        addFacetIfPresent(TreeNodeFacetImpl.create(
                cls,
                treeSuperNodeMethodHandler,
                treeSubNodesMethodHandlers,
                processClassContext.getFacetHolder()));

    }


    // -- FACET IMPL

    static class TreeNodeFacetImpl<T> extends FacetAbstract
    implements TreeNodeFacet<T> {

        static <T> Optional<TreeNodeFacet<T>> create(
                final Class<T> nodeType,
                final Optional<MethodHandle> superNodeMethodHandler,
                final Can<MethodHandle> subNodesMethodHandlers,
                final FacetHolder facetHolder){
            if(superNodeMethodHandler.isEmpty()
                    && subNodesMethodHandlers.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TreeNodeFacetImpl<>(
                    nodeType, superNodeMethodHandler, subNodesMethodHandlers, facetHolder));
        }

        @Getter(onMethod_={@Override}) @Accessors(fluent=true)
        private final Class<T> nodeType;

        private final Optional<MethodHandle> superNodeMethodHandle;
        private final Can<MethodHandle> subNodesMethodHandles;

        protected TreeNodeFacetImpl(final Class<T> nodeType,
                final @NonNull Optional<MethodHandle> superNodeMethodHandle,
                final @NonNull Can<MethodHandle> subNodesMethodHandles,
                final @NonNull FacetHolder facetHolder) {
            super(TreeNodeFacet.class, facetHolder, Precedence.DEFAULT);
            this.nodeType = nodeType;
            this.superNodeMethodHandle = superNodeMethodHandle;
            this.subNodesMethodHandles = subNodesMethodHandles;
        }

        @Override
        public Optional<Object> parentOf(final T node) {
            return superNodeMethodHandle
                    .flatMap(mh->{
                        try {
                            return ((Optional<?>)mh.invoke(node));
                        } catch (Throwable e) {
                            log.error("failed to invoke superNodeMethodHandler {}",
                                    mh.toString(), e);
                            return Optional.empty();
                        }
                    });
        }

        @Override
        public int childCountOf(final T node) {
            return subNodesMethodHandles.stream()
                .mapToInt(mh->{
                    try {
                        return ((Can<?>)mh.invoke(node)).size();
                    } catch (Throwable e) {
                        log.error("failed to invoke subNodesMethodHandle {}",
                                mh.toString(), e);
                        return 0;
                    }
                })
                .sum();
        }

        @Override
        public Stream<Object> childrenOf(final T node) {
            return subNodesMethodHandles.stream()
                .map(mh->{
                    try {
                        return (Can<?>)mh.invoke(node);
                    } catch (Throwable e) {
                        throw _Exceptions.unrecoverable(e);
                    }
                })
                .flatMap(Can::stream);
        }

        @Override
        public void visitAttributes(final BiConsumer<String, Object> visitor) {
            visitor.accept("nodeType", nodeType.getName());
            visitor.accept("superNodeMethodHandle", superNodeMethodHandle.map(MethodHandle::toString));
            visitor.accept("subNodesMethodHandles", subNodesMethodHandles.map(MethodHandle::toString));
        }
    }

}
