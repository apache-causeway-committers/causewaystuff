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
package io.github.causewaystuff.tooling.j2adoc.convert;

import java.util.Stack;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.commons.io.TextUtils;
import org.apache.causeway.valuetypes.asciidoc.builder.AsciiDocFactory;

import lombok.SneakyThrows;


final class HtmlToAsciiDoc {

    @SneakyThrows
    public static Document body(final Element body) {

        var adoc = AsciiDocFactory.doc();

        var helper = new BlockHelper(adoc);

        NodeTraversor.traverse(new NodeVisitor() {

            @Override
            public void head(Node node, int depth) {

                var tag = _Strings.nullToEmpty(node.nodeName()).toLowerCase();

                if(node instanceof TextNode) {

                    var textNode = (TextNode)node;

                    var text = helper.isPreFormatted()
                            ? skipBlankLinesAtHeadAndTail(textNode.getWholeText())
                            : textNode.text().trim();

                    if(!text.isBlank()) {
                        helper.blockAppend(text);
                    }
                    return;
                }

                switch(tag) {
                case "ul":
                    helper.nextList();
                    return;
                case "p":
                    helper.nextBlock();
                    return;
                case "pre":
                    helper.nextListingBlock();
                    helper.onPreHead();
                    return;
                case "li":
                    helper.nextListItem();
                    return;
                case "b":
                case "em":
                    helper.blockAppend(" *");
                    return;
                case "tt":
                case "code":
                    helper.blockAppend(" `");
                    return;
                case "i":
                    helper.blockAppend(" _");
                    return;
                }
            }


            @Override
            public void tail(Node node, int depth) {

                var tag = _Strings.nullToEmpty(node.nodeName()).toLowerCase();

                switch(tag) {
                case "ul":
                    helper.popList();
                    return;
                case "pre":
                    helper.onPreTail();
                case "p":
                case "li":
                    helper.pop();
                    return;
                case "b":
                case "em":
                    helper.blockAppend("* ");
                    return;
                case "tt":
                case "code":
                    helper.blockAppend("` ");
                    return;
                case "i":
                    helper.blockAppend("_ ");
                    return;
                }

            }


        }, body);

        return adoc;
    }

    // -- HELPER

    private static String skipBlankLinesAtHeadAndTail(String wholeText) {
        var headTrimmed = TextUtils.readLines(wholeText).stream()
                .dropWhile(String::isBlank)
                .collect(Can.toCan());
        return headTrimmed.reverse().stream()
            .dropWhile(String::isBlank)
            .collect(Can.toCan())
            .reverse()
            .join("\n");
    }

    private final static class BlockHelper {

        private final Stack<StructuralNode> nodeStack = new Stack<>();
        private final Stack<org.asciidoctor.ast.List> listStack = new Stack<>();


        // first element on the stack is the document, that is the the root of the adoc abstract syntax tree
        BlockHelper(final Document adoc){
            nodeStack.push(adoc);
        }

        void pop() {
            nodeStack.pop();
        }

        void popList() {
            nodeStack.pop();
            listStack.pop();
        }

        // create a new block on top of the current stack
        Block nextBlock() {
            var block = AsciiDocFactory.block(nodeStack.peek());
            nodeStack.push(block);
            return block;
        }

        // create a new block on top of the current stack
        Block nextListingBlock() {
            var block = AsciiDocFactory.listingBlock(nodeStack.peek(), "");
            nodeStack.push(block);
            return block;
        }

        // if the stack top is already a block reuse it or create a new one
        Block getBlock() {
            return (nodeStack.peek() instanceof Block)
                    ? (Block) nodeStack.peek()
                    : nextBlock();
        }

        void blockAppend(final String source) {
            var block = getBlock();
            block.setSource(block.getSource() + source);
        }

        org.asciidoctor.ast.List nextList() {
            var nextList = AsciiDocFactory.list(nodeStack.peek());
            nodeStack.push(nextList);
            listStack.push(nextList);
            return nextList;
        }

        void nextListItem() {
            var list = listStack.isEmpty()
                    ? nextList()
                    : listStack.peek();

            // pop until stack top points to list
            while(!list.equals(nodeStack.peek())) {
                nodeStack.pop();
            }
            var listItem = AsciiDocFactory.listItem(list);
            var openBlock = AsciiDocFactory.openBlock(listItem);
            nodeStack.push(openBlock);
        }

        // -- PRE HANDLING

        int preDepth = 0;

        void onPreHead() {
            ++preDepth;
        }

        void onPreTail() {
            --preDepth;
        }

        boolean isPreFormatted() {
            return preDepth>0;
        }

    }

}
