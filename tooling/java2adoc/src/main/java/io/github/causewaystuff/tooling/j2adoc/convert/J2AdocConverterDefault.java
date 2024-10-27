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

import java.util.Collections;
import java.util.List;

import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.commons.internal._Constants;

import io.github.causewaystuff.tooling.j2adoc.J2AdocContext;
import io.github.causewaystuff.tooling.j2adoc.J2AdocUnit;
import io.github.causewaystuff.tooling.j2adoc.format.MemberFormatter;
import io.github.causewaystuff.tooling.javamodel.ast.AnnotationMemberDeclarations;
import io.github.causewaystuff.tooling.javamodel.ast.ConstructorDeclarations;
import io.github.causewaystuff.tooling.javamodel.ast.EnumConstantDeclarations;
import io.github.causewaystuff.tooling.javamodel.ast.FieldDeclarations;
import io.github.causewaystuff.tooling.javamodel.ast.Javadocs;
import io.github.causewaystuff.tooling.javamodel.ast.MethodDeclarations;
import org.apache.causeway.valuetypes.asciidoc.builder.AsciiDocFactory;
import org.apache.causeway.valuetypes.asciidoc.builder.ast.SimpleBlock;

import lombok.NonNull;


public final class J2AdocConverterDefault extends J2AdocConverterAbstract {

    public static J2AdocConverter of(final J2AdocContext j2AdocContext) {
        return new J2AdocConverterDefault(j2AdocContext);
    }
    private J2AdocConverterDefault(final J2AdocContext j2AdocContext) {
        super(j2AdocContext);
    }

    @Override
    public String annotationMemberDeclaration(
            final @NonNull AnnotationMemberDeclaration amd,
            final @NonNull J2AdocUnit unit) {
        var isDeprecated = amd.getAnnotations().stream()
                .anyMatch(a->a.getNameAsString().equals("Deprecated"))
                || amd.getJavadoc()
                    .map(Javadocs::hasDeprecated)
                    .orElse(false);

        var memberNameFormat = isDeprecated
                ? j2aContext.getDeprecatedStaticMemberNameFormat()
                : j2aContext.getStaticMemberNameFormat();

        var annotMemberFormat =  new MemberFormatter(){}.getAnnotationMemberFormat();

        return String.format(annotMemberFormat,
                type(amd.getType(), unit),
                String.format(memberNameFormat, AnnotationMemberDeclarations.asNormalizedName(amd)));
    }

    @Override
    public String enumConstantDeclaration(final @NonNull EnumConstantDeclaration ecd) {
        var isDeprecated = ecd.getAnnotations().stream()
                .anyMatch(a->a.getNameAsString().equals("Deprecated"))
                || ecd.getJavadoc()
                    .map(Javadocs::hasDeprecated)
                    .orElse(false);

        var memberNameFormat = isDeprecated
                ? j2aContext.getDeprecatedStaticMemberNameFormat()
                : j2aContext.getStaticMemberNameFormat();

        var enumConstFormat =  new MemberFormatter(){}.getEnumConstantFormat();

        return String.format(enumConstFormat,
                String.format(memberNameFormat, EnumConstantDeclarations.asNormalized(ecd)));
    }

    @Override
    public String fieldDeclaration(
            final @NonNull FieldDeclaration fd,
            final @NonNull J2AdocUnit unit) {

        var isDeprecated = fd.getAnnotations().stream()
                .anyMatch(a->a.getNameAsString().equals("Deprecated"))
                || fd.getJavadoc()
                    .map(Javadocs::hasDeprecated)
                    .orElse(false);

        var memberNameFormat = isDeprecated
                ? fd.isStatic()
                        ? j2aContext.getDeprecatedStaticMemberNameFormat()
                        : j2aContext.getDeprecatedMemberNameFormat()
                : fd.isStatic()
                        ? j2aContext.getStaticMemberNameFormat()
                        : j2aContext.getMemberNameFormat();

        var fieldFormat =  new MemberFormatter(){}.getFieldFormat();

        return String.format(fieldFormat,
                type(fd.getCommonType(), unit),
                String.format(memberNameFormat, FieldDeclarations.asNormalizedName(fd)));
    }

    @Override
    public String constructorDeclaration(
            final @NonNull ConstructorDeclaration cd,
            final @NonNull J2AdocUnit unit) {

        var isDeprecated = cd.getAnnotations().stream()
                .anyMatch(a->a.getNameAsString().equals("Deprecated"))
                || cd.getJavadoc()
                    .map(Javadocs::hasDeprecated)
                    .orElse(false);

        var memberNameFormat = isDeprecated
                ? cd.isStatic()
                        ? j2aContext.getDeprecatedStaticMemberNameFormat()
                        : j2aContext.getDeprecatedMemberNameFormat()
                : cd.isStatic()
                        ? j2aContext.getStaticMemberNameFormat()
                        : j2aContext.getMemberNameFormat();

        var typeParams = ConstructorDeclarations.getTypeParameters(cd);

        var isGenericMember = !typeParams.isEmpty();

        var constructorFormat = isGenericMember
                ? new MemberFormatter(){}.getGenericConstructorFormat()
                : new MemberFormatter(){}.getConstructorFormat();

        var args = Can.<Object>of(
                isGenericMember ? typeParameters(typeParams) : null,  // Cans do ignored null
                String.format(memberNameFormat, ConstructorDeclarations.asNormalizedName(cd)),
                parameters(cd.getParameters().stream(), unit)
                );

        return String.format(constructorFormat, args.toArray(_Constants.emptyObjects));
    }

    @Override
    public String methodDeclaration(
            final @NonNull MethodDeclaration md,
            final @NonNull J2AdocUnit unit) {

        var isDeprecated = md.getAnnotations().stream()
                .anyMatch(a->a.getNameAsString().equals("Deprecated"))
                || md.getJavadoc()
                    .map(Javadocs::hasDeprecated)
                    .orElse(false);

        var memberNameFormat = isDeprecated
                ? md.isStatic()
                        ? j2aContext.getDeprecatedStaticMemberNameFormat()
                        : j2aContext.getDeprecatedMemberNameFormat()
                : md.isStatic()
                        ? j2aContext.getStaticMemberNameFormat()
                        : j2aContext.getMemberNameFormat();

        var typeParams = MethodDeclarations.getTypeParameters(md);

        var isGenericMember = !typeParams.isEmpty();

        var methodFormat = isGenericMember
                ? new MemberFormatter(){}.getGenericMethodFormat()
                : new MemberFormatter(){}.getMethodFormat();

        var args = Can.<Object>of(
                isGenericMember ? typeParameters(typeParams) : null,  // Cans do ignored null
                type(md.getType(), unit),
                String.format(memberNameFormat, MethodDeclarations.asNormalizedName(md)),
                parameters(md.getParameters().stream(), unit)
                );

        return String.format(methodFormat, args.toArray(_Constants.emptyObjects));

    }

    @Override
    public Document javadoc(
            final @NonNull Javadoc javadoc,
            final @NonNull J2AdocUnit unit,
            final @NonNull Mode mode) {

        var doc = AsciiDocFactory.doc();

        Javadocs.streamTagContent(javadoc, "deprecated")
        .findFirst()
        .map(javadocDescription->javadocDescription(javadocDescription, unit))
        .ifPresent(deprecatedAdoc->{

            var deprecatedBlock = AsciiDocFactory.warning(doc);
            deprecatedBlock.setSource("[red]#_deprecated:_#");
            deprecatedBlock.getBlocks().addAll(deprecatedAdoc.getBlocks());
        });

        var descriptionAdoc = javadocDescription(javadoc.getDescription(), unit);

        var blocks = descriptionAdoc.getBlocks();
        appendBlocks(mode, doc, blocks);

        return doc;
    }

    void appendBlocks(final Mode mode, final Document doc, final List<StructuralNode> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        final StructuralNode block = blocks.get(0);

        if (mode == Mode.FIRST_PARA_ONLY) {
            if (block instanceof SimpleBlock) {
                final SimpleBlock simpleBlock = (SimpleBlock) block;
                final List<String> lines = simpleBlock.getLines();
                if (!lines.isEmpty()) {
                    AsciiDocFactory.block(doc).setLines(Collections.singletonList(lines.get(0)));
                    return;
                }
            }
        }

        doc.getBlocks().addAll(blocks);
    }

    @Override
    protected String inlineTag(
            final @NonNull JavadocInlineTag inlineTag,
            final @NonNull J2AdocUnit unit) {

        var inlineContent = inlineTag.getContent().trim();

        switch(inlineTag.getType()) {
        case LINK:
            var referencedUnit = j2aContext.findUnit(inlineContent, unit).orElse(null);
            if(referencedUnit!=null) {
                return String.format(" %s ", j2aContext.xref(referencedUnit));
            }
        default:
            return String.format(" _%s_ ", inlineContent);
        }
    }

}
