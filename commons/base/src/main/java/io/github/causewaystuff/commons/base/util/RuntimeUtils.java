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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.inject.ServiceInjector;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.wrapper.WrapperFactory;
import org.apache.causeway.commons.internal.context._Context;
import org.apache.causeway.commons.internal.ioc._IocContainer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RuntimeUtils {

    @Getter(lazy=true)
    private final FactoryService factoryService = getIocContainer().get(FactoryService.class).orElseThrow();

    @Getter(lazy=true)
    private final ServiceInjector serviceInjector = getIocContainer().get(ServiceInjector.class).orElseThrow();

    @Getter(lazy=true)
    private final RepositoryService repositoryService = getIocContainer().get(RepositoryService.class).orElseThrow();

    @Getter(lazy=true)
    private final WrapperFactory wrapperFactory = getIocContainer().get(WrapperFactory.class).orElseThrow();

    /**
     * Invalidates cached values, potentially useful in case the underlying application context was refreshed.
     */
    public void invalidate() {
        ((AtomicReference<Object>)factoryService).set(null);
        ((AtomicReference<Object>)serviceInjector).set(null);
        ((AtomicReference<Object>)repositoryService).set(null);
        ((AtomicReference<Object>)wrapperFactory).set(null);
        ((AtomicReference<Object>)iocContainer).set(null);
    }

    // -- HELPER

    @Getter(lazy=true, value = AccessLevel.PRIVATE)
    private final _IocContainer iocContainer = iocContainer();

    //TODO provide friendly error messages (requires a valid/bootstrapped Spring context; otherwise will fail)
    @SneakyThrows
    private _IocContainer iocContainer() {
        var envClass = _Context
            .loadClass("org.apache.causeway.core.config.environment.CausewaySystemEnvironment");
        var env = _Context.getElseFail(envClass);
        final _IocContainer iocContainer = (_IocContainer) envClass.getMethod("getIocContainer")
                .invoke(env);
        return iocContainer;
    }

}
