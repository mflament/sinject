package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.ServiceResolver;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.exceptions.ServiceCreationException;
import org.yah.sinject.impl.builder.ResolvedDependencies;
import org.yah.sinject.impl.builder.ServiceDependencies;

import java.util.Objects;
import java.util.Optional;

public class MethodServiceDeclaration<T> extends AbstractServiceDeclaration<T> {

    public static <T> MethodServiceDeclaration<T> create(ServiceDeclaration<?> classDeclaration,
                                                         AnnotatedMethod method) {
        Objects.requireNonNull(classDeclaration, "classDeclaration is null");
        Objects.requireNonNull(method, "method is null");
        return new Builder<T>(classDeclaration, method).build();
    }

    private final ServiceDeclaration<?> instanceDeclaration;
    private final AnnotatedMethod method;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<InstanceSupplier<? extends T>> instanceSupplier;

    private MethodServiceDeclaration(Builder<T> builder) {
        super(builder);
        this.instanceDeclaration = builder.classDeclaration;
        this.method = builder.method;
    }

    @Override
    public Optional<InstanceSupplier<? extends T>> createInstanceSupplier(ServiceResolver resolver) {
        //noinspection OptionalAssignedToNull
        if (instanceSupplier == null) {
            instanceSupplier = Optional.ofNullable(createSupplier(resolver));
        }
        return instanceSupplier;
    }

    private InstanceSupplier<? extends T> createSupplier(ServiceResolver resolver) {
        final ServiceDependencies dependencies = method.createDependencies(instanceDeclaration);
        final ResolvedDependencies resolvedDependencies = dependencies.resolve(resolver);
        final MethodInvocationSupplier<?> methodInstanceSupplier = new MethodInvocationSupplier<>(method, resolvedDependencies);

        T optionalValue = null;
        if (method.isOptional()) {
            // for optional service, create the instance now in order to make the declaration
            // visible or not
            // if the Optional value is a Class, the service instantiation will still be done
            // when needed, we just need to know that it exists
            Optional<T> optional;
            try {
                //noinspection unchecked
                optional = (Optional<T>) methodInstanceSupplier.get();
            } catch (Exception e) {
                throw new ServiceCreationException(this, "error creating optional service", e);
            }

            if (optional.isEmpty())
                return null;
            optionalValue = optional.get();
        }

        if (method.isServiceClass()) {
            // method return value is a Class that will be auto injected and instantiated when needed
            // we need to ensure that the class constructor is resolved now to resolve missing dependencies

            // we have to call the method to get the service class, if not yet done during optional resolution
            //noinspection unchecked
            Class<? extends T> serviceClass = (Class<? extends T>) optionalValue;
            if (serviceClass == null) {
                try {
                    //noinspection unchecked
                    serviceClass = (Class<? extends T>) methodInstanceSupplier.get();
                } catch (Exception e) {
                    throw new ServiceCreationException(this, "error getting service class", e);
                }
            }

            return ClassInstanceDeclaration
                    .builder(serviceClass)
                    .build()
                    .createInstanceSupplier(resolver)
                    // ClassInstanceDeclaration always create a supplier
                    .orElseThrow(IllegalStateException::new);
        } else {
            //noinspection unchecked
            return (InstanceSupplier<T>) methodInstanceSupplier;
        }
    }

    public static class Builder<T> extends AbstractBuilder<T, Builder<T>> {
        private final ServiceDeclaration<?> classDeclaration;
        private final AnnotatedMethod method;

        private Builder(ServiceDeclaration<?> classDeclaration, AnnotatedMethod method) {
            super(method.getServiceType());
            this.classDeclaration = classDeclaration;
            this.method = method;
            withName(method.getName()).withPriority(method.getPriority());
        }

        public MethodServiceDeclaration<T> build() {
            return new MethodServiceDeclaration<>(this);
        }

        @Override
        protected Builder<T> getThis() {
            return this;
        }
    }
}
