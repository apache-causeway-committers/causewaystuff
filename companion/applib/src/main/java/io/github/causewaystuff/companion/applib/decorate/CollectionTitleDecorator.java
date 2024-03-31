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
package io.github.causewaystuff.companion.applib.decorate;

import org.apache.causeway.applib.annotation.TableDecorator;

public class CollectionTitleDecorator implements TableDecorator {

    /**
     * Works in cooperation with javascript, to be defined in your application.js:
     *
     * <pre>{@code
     * function highlight_dependants() {
     *   var spans = Array.from(document.getElementsByClassName('card-title'));
     *   spans.forEach(span => {
     *     var highlighted = span.innerHTML;
     *     highlighted = highlighted.replace("Dependent ", "Dependent <b>")
     *     highlighted = highlighted.replace(" Mapped By ", "</b> mapped by <b>") + "</b>"
     *     span.innerHTML = highlighted;
     *     });
     * }
     * }</pre>
     * <p>
     * @apiNote perhaps we can refactor this into a feature that works out of the box
     */
    @Override
    public String documentReadyJavaScript() {
        return "highlight_dependants()"; // as defined in application.js
    }
}
