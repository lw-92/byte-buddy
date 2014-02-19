package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.NamingStrategy;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.JunctionMethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class SubclassDynamicTypeBuilder<T> extends DynamicType.Builder.AbstractBase<T> {

    private static final int ASM_MANUAL = 0;

    private class SubclassFieldAnnotationTarget<T> extends AbstractDelegatingBuilder<T> implements FieldAnnotationTarget<T> {

        private final FieldToken fieldToken;
        private final FieldAttributeAppender.Factory attributeAppenderFactory;

        private SubclassFieldAnnotationTarget(FieldToken fieldToken, FieldAttributeAppender.Factory attributeAppenderFactory) {
            this.fieldToken = fieldToken;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        @Override
        protected DynamicType.Builder<T> materialize() {
            return new SubclassDynamicTypeBuilder<T>(classVersion,
                    namingStrategy,
                    superType,
                    interfaceTypes,
                    modifiers,
                    attributeAppender,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    instrumentationRegistry,
                    join(fieldTokens, fieldToken),
                    methodTokens);
        }

        @Override
        public FieldAnnotationTarget<T> attribute(FieldAttributeAppender.Factory attributeAppenderFactory) {
            return new SubclassFieldAnnotationTarget<T>(fieldToken,
                    new FieldAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        @Override
        public FieldAnnotationTarget<T> annotateField(Annotation annotation) {
            return attribute(new FieldAttributeAppender.ForAnnotation(annotation));
        }
    }

    private class SubclassMatchedMethodInterception<T> implements MatchedMethodInterception<T> {

        private final InterceptionRegistry.LatentDecision latentDecision;
        private final List<MethodToken> methodTokens;

        private SubclassMatchedMethodInterception(InterceptionRegistry.LatentDecision latentDecision, List<MethodToken> methodTokens) {
            this.latentDecision = latentDecision;
            this.methodTokens = methodTokens;
        }

        @Override
        public MethodAnnotationTarget<T> intercept(Instrumentation instrumentation) {
            return new SubclassMethodAnnotationTarget<T>(methodTokens,
                    latentDecision,
                    instrumentation,
                    MethodAttributeAppender.NoOp.INSTANCE);
        }
    }

    private class SubclassMethodAnnotationTarget<T> extends AbstractDelegatingBuilder<T> implements MethodAnnotationTarget<T> {

        private final List<MethodToken> methodTokens;
        private final InterceptionRegistry.LatentDecision latentDecision;
        private final Instrumentation instrumentation;
        private final MethodAttributeAppender.Factory attributeAppenderFactory;

        private SubclassMethodAnnotationTarget(List<MethodToken> methodTokens,
                                               InterceptionRegistry.LatentDecision latentDecision,
                                               Instrumentation instrumentation,
                                               MethodAttributeAppender.Factory attributeAppenderFactory) {
            this.latentDecision = latentDecision;
            this.methodTokens = methodTokens;
            this.instrumentation = instrumentation;
            this.attributeAppenderFactory = attributeAppenderFactory;
        }

        @Override
        protected DynamicType.Builder<T> materialize() {
            return new SubclassDynamicTypeBuilder<T>(classVersion,
                    namingStrategy,
                    superType,
                    interfaceTypes,
                    modifiers,
                    attributeAppender,
                    ignoredMethods,
                    classVisitorWrapperChain,
                    instrumentationRegistry.prepend(latentDecision, instrumentation, attributeAppenderFactory),
                    fieldTokens,
                    methodTokens);
        }

        @Override
        public MethodAnnotationTarget<T> attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new SubclassMethodAnnotationTarget<T>(
                    methodTokens,
                    latentDecision,
                    instrumentation,
                    new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        @Override
        public MethodAnnotationTarget<T> annotateMethod(Annotation annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(annotation));
        }

        @Override
        public MethodAnnotationTarget<T> annotateParameter(int parameterIndex, Annotation annotation) {
            return attribute(new MethodAttributeAppender.ForAnnotation(annotation, parameterIndex));
        }
    }

    private final ClassVersion classVersion;
    private final NamingStrategy namingStrategy;
    private final Class<?> superType;
    private final List<Class<?>> interfaceTypes;
    private final int modifiers;
    private final TypeAttributeAppender attributeAppender;
    private final MethodMatcher ignoredMethods;
    private final ClassVisitorWrapper.Chain classVisitorWrapperChain;
    private final InterceptionRegistry instrumentationRegistry;

    public SubclassDynamicTypeBuilder(ClassVersion classVersion,
                                      NamingStrategy namingStrategy,
                                      Class<?> superType,
                                      List<Class<?>> interfaceTypes,
                                      int modifiers,
                                      TypeAttributeAppender attributeAppender,
                                      MethodMatcher ignoredMethods,
                                      ClassVisitorWrapper.Chain classVisitorWrapperChain,
                                      InterceptionRegistry instrumentationRegistry,
                                      List<FieldToken> fieldTokens,
                                      List<MethodToken> methodTokens) {
        super(fieldTokens, methodTokens);
        this.classVersion = classVersion;
        this.namingStrategy = namingStrategy;
        this.superType = superType;
        this.interfaceTypes = interfaceTypes;
        this.modifiers = modifiers;
        this.attributeAppender = attributeAppender;
        this.ignoredMethods = ignoredMethods;
        this.classVisitorWrapperChain = classVisitorWrapperChain;
        this.instrumentationRegistry = instrumentationRegistry;
    }

    @Override
    public DynamicType.Builder<T> classVersion(int classVersion) {
        return new SubclassDynamicTypeBuilder<T>(new ClassVersion(classVersion),
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> implement(Class<?> interfaceType) {
        return new SubclassDynamicTypeBuilder<T>(classVersion,
                namingStrategy,
                superType,
                join(interfaceTypes, isInterface(nonNull(interfaceType))),
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> name(String name) {
        return new SubclassDynamicTypeBuilder<T>(classVersion,
                new NamingStrategy.Fixed(nonNull(name)),
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> modifier(ModifierContributor.ForType... modifier) {
        return new SubclassDynamicTypeBuilder<T>(classVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                resolveModifiers(InstrumentedType.TYPE_MODIFIER_MASK, modifier),
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain,
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> ignoreMethods(MethodMatcher ignoredMethods) {
        return new SubclassDynamicTypeBuilder<T>(classVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                new JunctionMethodMatcher.Conjunction(this.ignoredMethods, nonNull(ignoredMethods)),
                classVisitorWrapperChain,
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> attribute(TypeAttributeAppender attributeAppender) {
        return new SubclassDynamicTypeBuilder<T>(classVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                new TypeAttributeAppender.Compound(this.attributeAppender, nonNull(attributeAppender)),
                ignoredMethods,
                classVisitorWrapperChain,
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public DynamicType.Builder<T> annotateType(Annotation annotation) {
        return attribute(new TypeAttributeAppender.ForAnnotation(annotation));
    }

    @Override
    public DynamicType.Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
        return new SubclassDynamicTypeBuilder<T>(classVersion,
                namingStrategy,
                superType,
                interfaceTypes,
                modifiers,
                attributeAppender,
                ignoredMethods,
                classVisitorWrapperChain.append(nonNull(classVisitorWrapper)),
                instrumentationRegistry,
                fieldTokens,
                methodTokens);
    }

    @Override
    public FieldAnnotationTarget<T> defineField(String name,
                                                Class<?> fieldType,
                                                ModifierContributor.ForField... modifier) {
        FieldToken fieldToken = new FieldToken(name, fieldType, resolveModifiers(InstrumentedType.FIELD_MODIFIER_MASK, modifier));
        return new SubclassFieldAnnotationTarget<T>(fieldToken, FieldAttributeAppender.NoOp.INSTANCE);
    }

    @Override
    public MatchedMethodInterception<T> defineMethod(String name,
                                                     Class<?> returnType,
                                                     List<Class<?>> parameterTypes,
                                                     ModifierContributor.ForMethod... modifier) {
        MethodToken methodToken = new MethodToken(name,
                returnType,
                parameterTypes,
                resolveModifiers(InstrumentedType.METHOD_MODIFIER_MASK, modifier));
        return new SubclassMatchedMethodInterception<T>(methodToken, join(methodTokens, methodToken));
    }

    @Override
    public MatchedMethodInterception<T> method(MethodMatcher methodMatcher) {
        return new SubclassMatchedMethodInterception<T>(new InterceptionRegistry.LatentDecision.Simple(methodMatcher), methodTokens);
    }

    @Override
    public DynamicType.Unloaded<T> make() {
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL);
        ClassVisitor classVisitor = classVisitorWrapperChain.wrap(classWriter);
        InstrumentedType instrumentedType = new SubclassLoadedTypeInstrumentation(classVersion,
                superType,
                interfaceTypes,
                modifiers,
                namingStrategy);
        classVisitor.visit(classVersion.getVersionNumber(),
                instrumentedType.getModifiers(),
                instrumentedType.getInternalName(),
                null,
                instrumentedType.getSupertype().getInternalName(),
                instrumentedType.getInterfaces().toInternalNames());
//        MethodInterception.Handler handler = methodInterceptions.handler(instrumentedType);
//        for (MethodDescription method : instrumentedType.getReachableMethods().filter(not(ignoredMethods)
//                .and(new WritableMethodFilter(instrumentedType)))) {
//            handler.find(method).handle(classVisitor);
//        }
//        for (MethodDescription method : instrumentedType.getSupertype().getDeclaredMethods().filter(not(ignoredMethods)
//                .and(isConstructor()).and(not(isPrivate())))) {
//            handler.find(method).handle(classVisitor);
//        }
        classVisitor.visitEnd();
        return new DynamicType.Default.Unloaded<T>(instrumentedType.getName(),
                classWriter.toByteArray(),
                Collections.<Instrumentation.ClassLoadingCallback>emptyList(),
                Collections.<DynamicType<?>>emptySet());
    }
}
