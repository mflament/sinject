package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.Parametric;
import org.yah.sinject.ServiceResolver;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceInstanceFactory;
import org.yah.sinject.impl.builder.ResolvedDependencies;
import org.yah.sinject.impl.builder.ServiceDependencies;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public class DefaultServiceDeclaration<T> extends AbstractServiceDeclaration<T> {

    public static <T> Builder<T> builder(Class<T> serviceType) {
        return new Builder<>(serviceType);
    }

    public static <T> Builder<T> builder(Parametric<T> parametric) {
        return new Builder<>(parametric.getType());
    }

    public static <T> Builder<T> builder(Type serviceType) {
        return new Builder<>(serviceType);
    }

    private final ServiceDependencies dependencies;
    private final ServiceInstanceFactory<T> factory;

    public DefaultServiceDeclaration(Builder<T> builder) {
        super(builder);
        this.dependencies = Objects.requireNonNull(builder.dependencies);
        this.factory = Objects.requireNonNull(builder.factory);
    }

    @Override
    public Optional<InstanceSupplier<? extends T>> createInstanceSupplier(ServiceResolver resolver) {
        final ResolvedDependencies resolvedDependencies = dependencies.resolve(resolver);
        return Optional.of(() -> factory.create(resolvedDependencies));
    }

    public static class Builder<T> extends AbstractBuilder<T, Builder<T>> {
        public ServiceDependencies dependencies = ServiceDependencies.EMPTY;
        public ServiceInstanceFactory<T> factory;

        public Builder(Type type) {
            super(type);
        }

        public Builder<T> withFactory(ServiceInstanceFactory<T> factory) {
            this.factory = factory;
            return this;
        }

        public Builder<T> withDependencies(ServiceDependencies dependencies) {
            this.dependencies = Objects.requireNonNull(dependencies);
            return this;
        }

        public DefaultServiceDeclaration<T> build() {
            return new DefaultServiceDeclaration<>(this);
        }

        @Override
        protected Builder<T> getThis() {
            return this;
        }
    }

}
