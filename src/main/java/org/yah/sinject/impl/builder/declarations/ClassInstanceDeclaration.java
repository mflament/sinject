package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.ServiceResolver;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.exceptions.UnresolvedConstructorException;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Optional;

public class ClassInstanceDeclaration<T> extends AbstractServiceDeclaration<T> {

    public static <T> Builder<T> builder(Class<? extends T> serviceClass, Type type) {
        return new Builder<>(serviceClass, type);
    }

    public static <T> Builder<T> builder(Class<? extends T> serviceClass) {
        return builder(serviceClass, serviceClass);
    }

    private final CandidateConstuctors<? extends T> constructors;

    private ClassInstanceDeclaration(Builder<T> builder) {
        super(builder);
        this.constructors = builder.constructors;
    }

    @Override
    public Optional<InstanceSupplier<? extends T>> createInstanceSupplier(ServiceResolver resolver) {
        final ResolvedConstructor<? extends T> constructor = constructors.resolve(resolver)
                .orElseThrow(() -> new UnresolvedConstructorException(this, constructors));
        return Optional.of(new ClassInstanceSupplier<>(constructor));
    }

    public static class Builder<T> extends AbstractBuilder<T, Builder<T>> {

        private final CandidateConstuctors<? extends T> constructors;

        public Builder(Class<? extends T> serviceClass, Type type) {
            super(type);
            if (serviceClass.isInterface())
                throw new IllegalArgumentException(serviceClass + " is an interface");
            if (serviceClass.isArray())
                throw new IllegalArgumentException(serviceClass + " is an array");
            if (serviceClass.isEnum())
                throw new IllegalArgumentException(serviceClass + " is an enum");
            if (Modifier.isAbstract(serviceClass.getModifiers()))
                throw new IllegalArgumentException(serviceClass + " is absract");
            constructors = CandidateConstuctors.create(serviceClass);
            constructors.getName().ifPresent(this::withName);
            constructors.getPriority().ifPresent(this::withPriority);
        }

        public ClassInstanceDeclaration<T> build() {
            return new ClassInstanceDeclaration<>(this);
        }

        @Override
        protected Builder<T> getThis() {
            return this;
        }
    }
}
