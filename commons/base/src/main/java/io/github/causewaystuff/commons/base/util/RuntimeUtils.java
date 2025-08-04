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
package io.github.causewaystuff.commons.base.util;

import org.jspecify.annotations.Nullable;

import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.inject.ServiceInjector;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.wrapper.WrapperFactory;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.commons.internal.ioc.SpringContextHolder;
import org.apache.causeway.core.config.environment.CausewaySystemEnvironment;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RuntimeUtils {

    public void init(@Nullable CausewaySystemEnvironment env) {
        ENV = env;
        invalidate();
    }

    @Getter(lazy=true)
    private final FactoryService factoryService = holder().get(FactoryService.class).orElseThrow();

    @Getter(lazy=true)
    private final ServiceInjector serviceInjector = holder().get(ServiceInjector.class).orElseThrow();

    @Getter(lazy=true)
    private final RepositoryService repositoryService = holder().get(RepositoryService.class).orElseThrow();

    @Getter(lazy=true)
    private final WrapperFactory wrapperFactory = holder().get(WrapperFactory.class).orElseThrow();

    /**
     * Invalidates cached values, potentially useful in case the underlying application context was refreshed.
     */
    private void invalidate() {
        factoryService.set(null);
        serviceInjector.set(null);
        repositoryService.set(null);
        wrapperFactory.set(null);
    }

    // -- HELPER

    private CausewaySystemEnvironment ENV;

    @SneakyThrows
    private SpringContextHolder holder() {
        if(ENV==null) {
            throw _Exceptions.illegalState("RuntimeUtils must be initialized before use.");
        }
        return ENV.springContextHolder();
    }

}
