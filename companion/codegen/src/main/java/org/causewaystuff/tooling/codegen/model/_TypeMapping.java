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
package org.causewaystuff.tooling.codegen.model;

import org.causewaystuff.tooling.javapoet.ClassName;
import org.causewaystuff.tooling.javapoet.TypeName;

import org.apache.causeway.commons.internal.exceptions._Exceptions;

import lombok.experimental.UtilityClass;

@UtilityClass
class _TypeMapping {

    TypeName dbToJava(final String typeName, final boolean nullable) {
        if(typeName.startsWith("nvarchar")) {
            return ClassName.get("java.lang", "String");
        }
        if(typeName.equals("bit(1)")) {
            return nullable
                    ? ClassName.get("java.lang", "Boolean")
                    : TypeName.BOOLEAN;
        }
        if(typeName.startsWith("int")
                || typeName.startsWith("smallint")
                || typeName.startsWith("tinyint")) {
            return nullable
                    ? ClassName.get("java.lang", "Integer")
                    : TypeName.INT;
        }
        if(typeName.startsWith("float")) {
            return nullable
                    ? ClassName.get("java.lang", "Double")
                    : TypeName.DOUBLE;
        }
        if(typeName.equals("datetime")
                || typeName.equals("datetime(23)")) {
            return ClassName.get("java.sql", "Timestamp");
        }
        throw _Exceptions.unmatchedCase(typeName);
    }

    boolean isMaxLengthSuppressedFor(final String typeName) {
        return typeName.equals("bit(1)")
                || typeName.startsWith("int")
                || typeName.startsWith("smallint")
                || typeName.startsWith("tinyint")
                || typeName.startsWith("float")
                || typeName.startsWith("datetime");
    }

}
