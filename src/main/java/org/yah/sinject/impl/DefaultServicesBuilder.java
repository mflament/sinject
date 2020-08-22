package org.yah.sinject.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.yah.sinject.*;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.builder.ServiceTransformer;
import org.yah.sinject.exceptions.CircularDependencyException;
import org.yah.sinject.exceptions.ServiceResolutionException;
import org.yah.sinject.impl.builder.ServiceDeclarationBuilder;
import org.yah.sinject.exceptions.NoSuchServiceException;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultServicesBuilder {

    // get first type argument
    public static Type optionalArgument(Type type) {
        final Map<TypeVariable<?>, Type> arguments = TypeUtils.getTypeArguments(type, Optional.class);
        if (arguments.isEmpty())
            return Object.class;
        return arguments.values().iterator().next();
    }

    /**
     * Contains registered service declarations organized as a graph of dependencies
     */
    private final LinkedList<ServiceDeclaration<?>> declarations = new LinkedList<>();

    private Services parent;
    private String name;

    DefaultServicesBuilder() {
    }

    public DefaultServicesBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public DefaultServicesBuilder withParent(Services parent) {
        this.parent = parent;
        return this;
    }

    public <T> ServiceDeclarationBuilder<T> declare(Class<T> type) {
        return ServiceDeclarationBuilder.create(this, type);
    }

    public <T> ServiceDeclarationBuilder<T> declare(Parametric<T> type) {
        return ServiceDeclarationBuilder.create(this, type);
    }

    public DefaultServices build() {
        return new BuilderContext().build();
    }

    public <T> DefaultServicesBuilder register(ServiceDeclaration<T> declaration) {
        declarations.add(declaration);
        return this;
    }

    private List<ServiceDeclaration<ServiceTransformer>> transformerDeclarations() {
        //noinspection unchecked
        return declarations.stream()
                .filter(d -> d.isAssignableTo(ServiceTransformer.class))
                .map(d -> (ServiceDeclaration<ServiceTransformer>) d)
                .collect(Collectors.toList());
    }

    private class BuilderContext implements ServiceResolver {

        private final DefaultServices services;
        private final List<Service<ServiceTransformer>> transformers;
        private final List<ServiceDeclaration<?>> resolvingDeclarations;

        public BuilderContext() {
            services = new DefaultServices(parent);
            resolvingDeclarations = new ArrayList<>();
            transformers = new ArrayList<>();
        }

        public DefaultServices build() {
            // register the Services instance as a service itself
            declare(Services.class).withName(name).withInstance(new ExposedServices(services)).register();

            // collect definitions transformers from parent
            services.services(ServiceTransformer.class).forEach(transformers::add);

            // create new transformers
            transformerDeclarations().stream()
                    .map(this::createService)
                    .filter(Objects::nonNull)
                    .forEach(transformers::add);

            // sort transformers to respect priority with parent transformers
            Collections.sort(transformers);

            // poll remaining configurations
            while (!declarations.isEmpty()) {
                final ServiceDeclaration<?> declaration = declarations.peek();
                createService(declaration);
            }

            services.freeze();
            return services;
        }

        @Override
        public Service<?> service(String name, Type type) throws ServiceResolutionException {
            try {
                return services.service(name, type);
            } catch (ServiceResolutionException e) {
                ServiceDeclaration<?> declaration = declaration(name, type);
                if (declaration != null)
                    return createService(declaration);
                if (isResolving(name, type))
                    throw new CircularDependencyException(resolvingDeclarations);
                throw new NoSuchServiceException(name, type);
            }
        }

        private boolean isResolving(String name, Type type) {
            return resolvingDeclarations.stream().anyMatch(d -> d.match(name, type));
        }

        private ServiceDeclaration<?> declaration(String name, Type type) {
            return declarations.stream()
                    .filter(d -> d.match(name, type))
                    .findFirst()
                    .orElse(null);
        }

        private <T> Service<T> createService(ServiceDeclaration<T> declaration) {
            resolvingDeclarations.add(declaration);
            try {
                if (!declarations.remove(declaration))
                    throw new IllegalArgumentException("declaration " + declaration + " not found in declarations");

                ServiceDeclaration<T> current = declaration;
                for (Service<ServiceTransformer> transformerService : transformers) {
                    final ServiceTransformer transformer = transformerService.get();
                    current = transformer.transform(current);
                }
                return declaration.createInstanceSupplier(this)
                        .map(supplier -> services.add(declaration, supplier))
                        .orElse(null);
            } finally {
                resolvingDeclarations.remove(resolvingDeclarations.size() - 1);
            }
        }

    }


    private static class ExposedServices implements Services {
        private final Services delegate;

        public ExposedServices(Services delegate) {
            this.delegate = delegate;
        }

        @Override
        public <V> TypedServices<V> services(Class<? super V> type) {
            return delegate.services(type);
        }

        @Override
        public <V> TypedServices<V> services(Parametric<? super V> parametric) {
            return delegate.services(parametric);
        }

        @Override
        public void close() {
            // prevent close
        }

        @Override
        @Nonnull
        public Iterator<Service<?>> iterator() {
            return delegate.iterator();
        }

        @Override
        public Service<?> service(String name, Type type) throws ServiceResolutionException {
            return delegate.service(name, type);
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o == delegate;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
