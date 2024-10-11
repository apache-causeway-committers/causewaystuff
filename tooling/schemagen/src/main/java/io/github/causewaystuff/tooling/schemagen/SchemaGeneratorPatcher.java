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
package io.github.causewaystuff.tooling.schemagen;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.types.ResolvedObjectType;
import com.github.victools.jsonschema.generator.TypeContext;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.reflection._Reflect;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
class SchemaGeneratorPatcher {

    /**
     * Patch the type resolver with our own, to support annotation types.
     * @return
     */
    @SneakyThrows
    TypeContext patch(final TypeContext typeContext) {
        _Reflect.setFieldOn(TypeContext.class.getDeclaredField("typeResolver"), typeContext, new TypeResolver2());
        return typeContext;
    }

    // -- HELPER

    private static class ResolvedType2 extends ResolvedObjectType {
        public ResolvedType2(
                final Class<?> erased,
                final TypeBindings bindings,
                final ResolvedType superClass,
                final List<ResolvedType> interfaces,
                final Can<RawMethod> memberMethods) {
            super(erased, bindings, superClass, interfaces);
            this._memberMethods = memberMethods.toArray(new RawMethod[0]);
        }
        @Override
        public synchronized List<RawMethod> getMemberMethods() {
            return super.getMemberMethods();
        }
    }

    private static class TypeResolver2 extends TypeResolver {
        private static final long serialVersionUID = 1L;
        @Override
        public ResolvedType resolve(final Type type, final Type... typeParameters) {
            var t = super.resolve(type, typeParameters);
            // intercept annotation types
            if(t.getErasedType().isAnnotation()) {
                var methods = Can.ofArray(t.getErasedType().getMethods())
                        .stream()
                        .limit(16)
                        .map(method->new RawMethod(t, method))
                        .collect(Can.toCan());
                return new ResolvedType2(t.getErasedType(), t.getTypeBindings(), null, null, methods);
            }
            return t;
        }
    }

}
