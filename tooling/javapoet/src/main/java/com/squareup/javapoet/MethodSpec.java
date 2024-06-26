/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/**
 * A generated constructor or method declaration.
 * @implNote Monkey patched from <a href="https://github.com/square/javapoet/pull/840/files">square</a>,
 *      as there is no support for <i>Java Records</i> with <i>Spring Framework</i> yet.
 */
public final class MethodSpec {
  static final String CONSTRUCTOR = "<init>";

  public final String name;
  public final CodeBlock javadoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final List<TypeVariableName> typeVariables;
  public final TypeName returnType;
  public final List<ParameterSpec> parameters;
  public final boolean varargs;
  public final List<TypeName> exceptions;
  public final CodeBlock code;
  public final CodeBlock defaultValue;

  private MethodSpec(final Builder builder) {
    CodeBlock code = builder.code.build();
    checkArgument(code.isEmpty() || !builder.modifiers.contains(Modifier.ABSTRACT),
        "abstract method %s cannot have code", builder.name);
    checkArgument(!builder.varargs || lastParameterIsArray(builder.parameters),
        "last parameter of varargs method %s must be an array", builder.name);

    this.name = checkNotNull(builder.name, "name == null");
    this.javadoc = builder.javadoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.typeVariables = Util.immutableList(builder.typeVariables);
    this.returnType = builder.returnType;
    this.parameters = Util.immutableList(builder.parameters);
    this.varargs = builder.varargs;
    this.exceptions = Util.immutableList(builder.exceptions);
    this.defaultValue = builder.defaultValue;
    this.code = code;
  }

  private boolean lastParameterIsArray(final List<ParameterSpec> parameters) {
    return !parameters.isEmpty()
        && TypeName.asArray((parameters.get(parameters.size() - 1).type)) != null;
  }

  void emit(final CodeWriter codeWriter, final String enclosingName, final Set<Modifier> implicitModifiers,
      final boolean compactConstructor)
      throws IOException {
    codeWriter.emitJavadoc(javadocWithParameters());
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);

    if (!typeVariables.isEmpty()) {
      codeWriter.emitTypeVariables(typeVariables);
      codeWriter.emit(" ");
    }

    if (compactConstructor) {
      codeWriter.emit("$L", enclosingName);
    } else {
      if (isConstructor()) {
        codeWriter.emit("$L", enclosingName);
      } else {
        codeWriter.emit("$T $L", returnType, name);
      }

      emitParameters(codeWriter, parameters, varargs);
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      codeWriter.emit(" default ");
      codeWriter.emit(defaultValue);
    }

    if (!exceptions.isEmpty()) {
      codeWriter.emitWrappingSpace().emit("throws");
      boolean firstException = true;
      for (TypeName exception : exceptions) {
        if (!firstException) codeWriter.emit(",");
        codeWriter.emitWrappingSpace().emit("$T", exception);
        firstException = false;
      }
    }

    if (hasModifier(Modifier.ABSTRACT)) {
      codeWriter.emit(";\n");
    } else if (hasModifier(Modifier.NATIVE)) {
      // Code is allowed to support stuff like GWT JSNI.
      codeWriter.emit(code);
      codeWriter.emit(";\n");
    } else {
      codeWriter.emit(" {\n");

      codeWriter.indent();
      codeWriter.emit(code, true);
      codeWriter.unindent();

      codeWriter.emit("}\n");
    }
    codeWriter.popTypeVariables(typeVariables);
  }

  private CodeBlock javadocWithParameters() {
    return makeJavadocWithParameters(javadoc, parameters);
  }

  static CodeBlock makeJavadocWithParameters(final CodeBlock javadoc,
      final List<ParameterSpec> parameters) {
    CodeBlock.Builder builder = javadoc.toBuilder();
    boolean emitTagNewline = true;
    for (ParameterSpec parameterSpec : parameters) {
      if (!parameterSpec.javadoc.isEmpty()) {
        // Emit a new line before @param section only if the method javadoc is present.
        if (emitTagNewline && !javadoc.isEmpty()) builder.add("\n");
        emitTagNewline = false;
        builder.add("@param $L $L\n", parameterSpec.name, parameterSpec.javadoc);
      }
    }
    return builder.build();
  }

  static void emitParameters(final CodeWriter codeWriter, final List<ParameterSpec> parameters,
      final boolean varargs) throws IOException {
    codeWriter.emit(CodeBlock.of("($Z"));

    if(parameters.size() == 1) {
        // single line style
        parameters.get(0).emit(codeWriter, varargs);
    } else {
        final int lastIndex = parameters.size()-1;
        int paramIndex = 0;

        for (ParameterSpec parameter: parameters) {
            boolean isFirst = paramIndex == 0;
            boolean isLast = paramIndex == lastIndex;
            boolean isMiddle = !isFirst && !isLast;
            if (isFirst) {
                codeWriter.emit("\n");
                codeWriter.indent().indent();
                emitParameter(parameter, codeWriter, false);
                codeWriter.emit(",\n");
            } else if (isMiddle) {
                emitParameter(parameter, codeWriter, false);
                codeWriter.emit(",\n");
            } else {
                emitParameter(parameter, codeWriter, varargs);
                codeWriter.unindent().unindent();
            }
            paramIndex++;
        }
    }

    codeWriter.emit(")");
  }

  static void emitParameter(final ParameterSpec param, final CodeWriter codeWriter, final boolean varargs) throws IOException {
      codeWriter.emitAnnotations(param.annotations, false);
      codeWriter.emitModifiers(param.modifiers);
      if (varargs) {
        TypeName.asArray(param.type).emit(codeWriter, true);
      } else {
          param.type.emit(codeWriter);
      }
      codeWriter.emit(" $L", param.name);
    }

  public boolean hasModifier(final Modifier modifier) {
    return modifiers.contains(modifier);
  }

  public boolean isConstructor() {
    return name.equals(CONSTRUCTOR);
  }

  @Override public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    return toString().hashCode();
  }

  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, "Constructor", Collections.emptySet(), false);
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public static Builder methodBuilder(final String name) {
    return new Builder(name);
  }

  public static Builder constructorBuilder() {
    return new Builder(CONSTRUCTOR);
  }

  /**
   * Returns a new method spec builder that overrides {@code method}.
   *
   * <p>This will copy its visibility modifiers, type parameters, return type, name, parameters, and
   * throws declarations. An {@link Override} annotation will be added.
   *
   * <p>Note that in JavaPoet 1.2 through 1.7 this method retained annotations from the method and
   * parameters of the overridden method. Since JavaPoet 1.8 annotations must be added separately.
   */
  public static Builder overriding(final ExecutableElement method) {
    checkNotNull(method, "method == null");

    Element enclosingClass = method.getEnclosingElement();
    if (enclosingClass.getModifiers().contains(Modifier.FINAL)) {
      throw new IllegalArgumentException("Cannot override method on final class " + enclosingClass);
    }

    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)
        || modifiers.contains(Modifier.FINAL)
        || modifiers.contains(Modifier.STATIC)) {
      throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
    }

    String methodName = method.getSimpleName().toString();
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

    methodBuilder.addAnnotation(Override.class);

    modifiers = new LinkedHashSet<>(modifiers);
    modifiers.remove(Modifier.ABSTRACT);
    modifiers.remove(Modifier.DEFAULT);
    methodBuilder.addModifiers(modifiers);

    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      TypeVariable var = (TypeVariable) typeParameterElement.asType();
      methodBuilder.addTypeVariable(TypeVariableName.get(var));
    }

    methodBuilder.returns(TypeName.get(method.getReturnType()));
    methodBuilder.addParameters(ParameterSpec.parametersOf(method));
    methodBuilder.varargs(method.isVarArgs());

    for (TypeMirror thrownType : method.getThrownTypes()) {
      methodBuilder.addException(TypeName.get(thrownType));
    }

    return methodBuilder;
  }

  /**
   * Returns a new method spec builder that overrides {@code method} as a member of {@code
   * enclosing}. This will resolve type parameters: for example overriding {@link
   * Comparable#compareTo} in a type that implements {@code Comparable<Movie>}, the {@code T}
   * parameter will be resolved to {@code Movie}.
   *
   * <p>This will copy its visibility modifiers, type parameters, return type, name, parameters, and
   * throws declarations. An {@link Override} annotation will be added.
   *
   * <p>Note that in JavaPoet 1.2 through 1.7 this method retained annotations from the method and
   * parameters of the overridden method. Since JavaPoet 1.8 annotations must be added separately.
   */
  public static Builder overriding(
      final ExecutableElement method, final DeclaredType enclosing, final Types types) {
    ExecutableType executableType = (ExecutableType) types.asMemberOf(enclosing, method);
    List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();
    List<? extends TypeMirror> resolvedThrownTypes = executableType.getThrownTypes();
    TypeMirror resolvedReturnType = executableType.getReturnType();

    Builder builder = overriding(method);
    builder.returns(TypeName.get(resolvedReturnType));
    for (int i = 0, size = builder.parameters.size(); i < size; i++) {
      ParameterSpec parameter = builder.parameters.get(i);
      TypeName type = TypeName.get(resolvedParameterTypes.get(i));
      builder.parameters.set(i, parameter.toBuilder(type, parameter.name).build());
    }
    builder.exceptions.clear();
    for (int i = 0, size = resolvedThrownTypes.size(); i < size; i++) {
      builder.addException(TypeName.get(resolvedThrownTypes.get(i)));
    }

    return builder;
  }

  public Builder toBuilder() {
    Builder builder = new Builder(name);
    builder.javadoc.add(javadoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.typeVariables.addAll(typeVariables);
    builder.returnType = returnType;
    builder.parameters.addAll(parameters);
    builder.exceptions.addAll(exceptions);
    builder.code.add(code);
    builder.varargs = varargs;
    builder.defaultValue = defaultValue;
    return builder;
  }

  public static final class Builder {
    private String name;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private TypeName returnType;
    private final Set<TypeName> exceptions = new LinkedHashSet<>();
    private final CodeBlock.Builder code = CodeBlock.builder();
    private boolean varargs;
    private CodeBlock defaultValue;

    public final List<TypeVariableName> typeVariables = new ArrayList<>();
    public final List<AnnotationSpec> annotations = new ArrayList<>();
    public final List<Modifier> modifiers = new ArrayList<>();
    public final List<ParameterSpec> parameters = new ArrayList<>();

    private Builder(final String name) {
      setName(name);
    }

    public Builder setName(final String name) {
      checkNotNull(name, "name == null");
      checkArgument(name.equals(CONSTRUCTOR) || SourceVersion.isName(name),
          "not a valid name: %s", name);
      this.name = name;
      this.returnType = name.equals(CONSTRUCTOR) ? null : TypeName.VOID;
      return this;
    }

    public Builder addJavadoc(final String format, final Object... args) {
      javadoc.add(format, args);
      return this;
    }

    public Builder addJavadoc(final CodeBlock block) {
      javadoc.add(block);
      return this;
    }

    public Builder addAnnotations(final Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    public Builder addAnnotation(final AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(final ClassName annotation) {
      this.annotations.add(AnnotationSpec.builder(annotation).build());
      return this;
    }

    public Builder addAnnotation(final Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    public Builder addModifiers(final Modifier... modifiers) {
      checkNotNull(modifiers, "modifiers == null");
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder addModifiers(final Iterable<Modifier> modifiers) {
      checkNotNull(modifiers, "modifiers == null");
      for (Modifier modifier : modifiers) {
        this.modifiers.add(modifier);
      }
      return this;
    }

    public Builder addTypeVariables(final Iterable<TypeVariableName> typeVariables) {
      checkArgument(typeVariables != null, "typeVariables == null");
      for (TypeVariableName typeVariable : typeVariables) {
        this.typeVariables.add(typeVariable);
      }
      return this;
    }

    public Builder addTypeVariable(final TypeVariableName typeVariable) {
      typeVariables.add(typeVariable);
      return this;
    }

    public Builder returns(final TypeName returnType) {
      checkState(!name.equals(CONSTRUCTOR), "constructor cannot have return type.");
      this.returnType = returnType;
      return this;
    }

    public Builder returns(final Type returnType) {
      return returns(TypeName.get(returnType));
    }

    public Builder addParameters(final Iterable<ParameterSpec> parameterSpecs) {
      checkArgument(parameterSpecs != null, "parameterSpecs == null");
      for (ParameterSpec parameterSpec : parameterSpecs) {
        this.parameters.add(parameterSpec);
      }
      return this;
    }

    public Builder addParameter(final ParameterSpec parameterSpec) {
      this.parameters.add(parameterSpec);
      return this;
    }

    public Builder addParameter(final TypeName type, final String name, final Modifier... modifiers) {
      return addParameter(ParameterSpec.builder(type, name, modifiers).build());
    }

    public Builder addParameter(final Type type, final String name, final Modifier... modifiers) {
      return addParameter(TypeName.get(type), name, modifiers);
    }

    public Builder varargs() {
      return varargs(true);
    }

    public Builder varargs(final boolean varargs) {
      this.varargs = varargs;
      return this;
    }

    public Builder addExceptions(final Iterable<? extends TypeName> exceptions) {
      checkArgument(exceptions != null, "exceptions == null");
      for (TypeName exception : exceptions) {
        this.exceptions.add(exception);
      }
      return this;
    }

    public Builder addException(final TypeName exception) {
      this.exceptions.add(exception);
      return this;
    }

    public Builder addException(final Type exception) {
      return addException(TypeName.get(exception));
    }

    public Builder addCode(final String format, final Object... args) {
      code.add(format, args);
      return this;
    }

    public Builder addNamedCode(final String format, final Map<String, ?> args) {
      code.addNamed(format, args);
      return this;
    }

    public Builder addCode(final CodeBlock codeBlock) {
      code.add(codeBlock);
      return this;
    }

    public Builder addComment(final String format, final Object... args) {
      code.add("// " + format + "\n", args);
      return this;
    }

    public Builder defaultValue(final String format, final Object... args) {
      return defaultValue(CodeBlock.of(format, args));
    }

    public Builder defaultValue(final CodeBlock codeBlock) {
      checkState(this.defaultValue == null, "defaultValue was already set");
      this.defaultValue = checkNotNull(codeBlock, "codeBlock == null");
      return this;
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * Shouldn't contain braces or newline characters.
     */
    public Builder beginControlFlow(final String controlFlow, final Object... args) {
      code.beginControlFlow(controlFlow, args);
      return this;
    }

    /**
     * @param codeBlock the control flow construct and its code, such as "if (foo == 5)".
     * Shouldn't contain braces or newline characters.
     */
    public Builder beginControlFlow(final CodeBlock codeBlock) {
      return beginControlFlow("$L", codeBlock);
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *     Shouldn't contain braces or newline characters.
     */
    public Builder nextControlFlow(final String controlFlow, final Object... args) {
      code.nextControlFlow(controlFlow, args);
      return this;
    }

    /**
     * @param codeBlock the control flow construct and its code, such as "else if (foo == 10)".
     *     Shouldn't contain braces or newline characters.
     */
    public Builder nextControlFlow(final CodeBlock codeBlock) {
      return nextControlFlow("$L", codeBlock);
    }

    public Builder endControlFlow() {
      code.endControlFlow();
      return this;
    }

    /**
     * @param controlFlow the optional control flow construct and its code, such as
     *     "while(foo == 20)". Only used for "do/while" control flows.
     */
    public Builder endControlFlow(final String controlFlow, final Object... args) {
      code.endControlFlow(controlFlow, args);
      return this;
    }

    /**
     * @param codeBlock the optional control flow construct and its code, such as
     *     "while(foo == 20)". Only used for "do/while" control flows.
     */
    public Builder endControlFlow(final CodeBlock codeBlock) {
      return endControlFlow("$L", codeBlock);
    }

    public Builder addStatement(final String format, final Object... args) {
      code.addStatement(format, args);
      return this;
    }

    public Builder addStatement(final CodeBlock codeBlock) {
      code.addStatement(codeBlock);
      return this;
    }

    public MethodSpec build() {
      return new MethodSpec(this);
    }
  }
}