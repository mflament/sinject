package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.ServiceResolver;
import org.yah.sinject.annotations.Service;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.exceptions.UnresolvedConstructorException;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

import static org.apache.commons.lang3.reflect.TypeUtils.getRawType;

public class ClassInstanceDeclaration<T> extends AbstractServiceDeclaration<T> {

    public static <T> Builder<T> builder(Class<? extends T> serviceClass, Type type) {
        return new Builder<>(serviceClass, type);
    }

    public static <T> Builder<T> builder(Class<? extends T> serviceClass) {
        return builder(serviceClass, serviceClass);
    }

    public static boolean hasNestedDeclarations(ServiceDeclaration<?> serviceDeclaration) {
        final Type type = serviceDeclaration.type();
        final Class<?> rawType = getRawType(type, null);
        if (isInstantiable(rawType)) {
            return Arrays.stream(rawType.getDeclaredMethods())
                    .anyMatch(m -> m.getAnnotation(Service.class) != null);
        }
        return false;
    }

    private static boolean isInstantiable(Class<?> serviceClass) {
        try {
            checkInstantiable(serviceClass);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void checkInstantiable(Class<?> serviceClass) {
        if (serviceClass.isInterface())
            throw new IllegalArgumentException(serviceClass + " is an interface");
        if (serviceClass.isArray())
            throw new IllegalArgumentException(serviceClass + " is an array");
        if (serviceClass.isEnum())
            throw new IllegalArgumentException(serviceClass + " is an enum");
        if (Modifier.isAbstract(serviceClass.getModifiers()))
            throw new IllegalArgumentException(serviceClass + " is absract");
    }
    private final CandidateConstuctors<? extends T> constructors;

    private ClassInstanceDeclaration(Builder<T> builder) {
        super(builder);
        constructors = CandidateConstuctors.create(builder.serviceClass);
    }

    @Override
    public Optional<InstanceSupplier<? extends T>> createInstanceSupplier(ServiceResolver resolver) {
        final ResolvedConstructor<? extends T> constructor = constructors.resolve(resolver)
                .orElseThrow(() -> new UnresolvedConstructorException(this, constructors));
        return Optional.of(new ClassInstanceSupplier<>(constructor));
    }

    public static class Builder<T> extends AbstractBuilder<T, Builder<T>> {
        private final Class<? extends T> serviceClass;

        public Builder(Class<? extends T> serviceClass, Type type) {
            super(type);
            checkInstantiable(serviceClass);
            this.serviceClass = serviceClass;
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
