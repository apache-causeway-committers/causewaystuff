/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package io.github.causewaystuff.tooling.cli.projdoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectDocModel_sanitizeDescription_Test {

    @Test
    void strips() {

        final String str = ProjectDocModel.sanitizeDescription(
                """
                    JDO Spring integration.
                    	\t
                    		This is a fork of the Spring ORM JDO sources at github,\s
                            for which support had been dropped back in 2016 [1].
                    	\t
                    		Credits to the original authors.
                    	\t
                    		[1] https://github.com/spring-projects/spring-framework/issues/18702""");

        assertEquals(str,
                """
                    JDO Spring integration.

                    This is a fork of the Spring ORM JDO sources at github,
                    for which support had been dropped back in 2016 [1].

                    Credits to the original authors.

                    [1] https://github.com/spring-projects/spring-framework/issues/18702""");
    }
}
