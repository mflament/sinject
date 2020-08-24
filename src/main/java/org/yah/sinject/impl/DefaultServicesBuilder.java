package org.yah.sinject.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.yah.sinject.*;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.builder.ServiceDeclarationTransformer;
import org.yah.sinject.builder.ServiceInstanceTransformer;
import org.yah.sinject.exceptions.CircularDependencyException;
import org.yah.sinject.exceptions.NoSuchServiceException;
import org.yah.sinject.exceptions.ServiceResolutionException;
import org.yah.sinject.impl.builder.ServiceDeclarationBuilder;
import org.yah.sinject.impl.builder.declarations.AnnotatedMethod;
import org.yah.sinject.impl.builder.declarations.MethodServiceDeclaration;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.reflect.TypeUtils.getRawType;

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


    private class BuilderContext implements ServiceResolver {

        private final DefaultServices services;
        private final List<Service<ServiceDeclarationTransformer>> declarationTransformers;
        private final List<Service<ServiceInstanceTransformer>> instanceTransformers;
        private final List<ServiceDeclaration<?>> resolvingDeclarations;

        public BuilderContext() {
            services = new DefaultServices(parent);
            resolvingDeclarations = new ArrayList<>();
            declarationTransformers = new ArrayList<>();
            instanceTransformers = new ArrayList<>();
        }

        public DefaultServices build() {
            // register the Services instance as a service itself
            declare(Services.class)
                    .withName(name)
                    .withInstance(new ExposedServices(services))
                    .register();

            // lookup any service that contains nested annotated methods,
            LinkedList<ServiceDeclaration<?>> remainings = new LinkedList<>(declarations);
            while (!remainings.isEmpty()) {
                final ServiceDeclaration<?> declaration = remainings.poll();
                final Collection<ServiceDeclaration<?>> newDeclarations = scanMethods(declaration);
                newDeclarations.forEach(newDeclaration -> {
                    remainings.offer(newDeclaration);
                    register(newDeclaration);
                });
            }

            // collect transformers from parent
            services.services(ServiceDeclarationTransformer.class).forEach(declarationTransformers::add);
            services.services(ServiceInstanceTransformer.class).forEach(instanceTransformers::add);

            // create new declarations transformers services
            // they will be processed by:
            // - any existing parent declaration and instance transformers
            // - newly created transformers
            // - not the new instance transformer, declaration transformer take precedence over instance transformers
            prepareTransformers(ServiceDeclarationTransformer.class, declarationTransformers);

            // collect new instance transformers
            // they will all be processed by all declaration transformers (new and existing)
            // they also will be processed by any previously created instance transformers
            prepareTransformers(ServiceInstanceTransformer.class, instanceTransformers);

            // poll remaining configurations
            while (!declarations.isEmpty()) {
                final ServiceDeclaration<?> declaration = declarations.peek();
                transformAndCreateService(declaration);
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
                    return transformAndCreateService(declaration);
                if (isResolving(name, type))
                    throw new CircularDependencyException(resolvingDeclarations);
                throw new NoSuchServiceException(name, type);
            }
        }

        private <T> void prepareTransformers(Class<T> type, List<Service<T>> target) {
            final List<ServiceDeclaration<?>> transformerDeclarations = declarations.stream()
                    .filter(d -> d.isAssignableTo(type))
                    .collect(Collectors.toList());

            //noinspection unchecked
            transformerDeclarations.stream()
                    .map(this::transformAndCreateService)
                    // since service creation can change service type, we need to check again
                    .filter(s -> s != null && s.isAssignableTo(type))
                    .forEach(s -> target.add((Service<T>) s));
            Collections.sort(target);
        }

        private Collection<ServiceDeclaration<?>> scanMethods(ServiceDeclaration<?> serviceDeclaration) {
            // walk up class hierarchy to collect any annotated methods
            Class<?> current = getRawType(serviceDeclaration.type(), null);
            final List<ServiceDeclaration<?>> methodDeclarations = new ArrayList<>();
            while (current != null) {
                Arrays.stream(current.getDeclaredMethods())
                        .map(this::createAnnotatedMethod)
                        .filter(Objects::nonNull)
                        .map(method -> MethodServiceDeclaration.create(serviceDeclaration, method))
                        .forEach(methodDeclarations::add);
                current = current.getSuperclass();
            }
            return methodDeclarations;
        }

        private AnnotatedMethod createAnnotatedMethod(Method method) {
            final org.yah.sinject.annotations.Service annotation = method
                    .getAnnotation(org.yah.sinject.annotations.Service.class);
            if (annotation != null)
                return new AnnotatedMethod(method, annotation);
            return null;
        }


        private ServiceDeclaration<?> declaration(String name, Type type) {
            return declarations.stream()
                    .filter(d -> d.match(name, type))
                    .findFirst()
                    .orElse(null);
        }

        private Service<?> transformAndCreateService(ServiceDeclaration<?> declaration) {
            resolvingDeclarations.add(declaration);
            try {
                if (!declarations.remove(declaration))
                    throw new IllegalArgumentException("declaration " + declaration + " not found in declarations");
                declaration = transformDeclaration(declaration);
                return createService(declaration);
            } finally {
                resolvingDeclarations.remove(resolvingDeclarations.size() - 1);
            }
        }

        private <T> Service<T> createService(ServiceDeclaration<T> declaration) {
            final InstanceSupplier<? extends T> instanceSupplier = declaration
                    .createInstanceSupplier(this)
                    .orElse(null);
            if (instanceSupplier == null)
                return null;
            final List<Service<ServiceInstanceTransformer>> transformers = new ArrayList<>(instanceTransformers);
            InstanceSupplier<? extends T> supplier = () -> transformInstance(transformers, declaration, instanceSupplier
                    .get());
            return this.services.add(declaration, supplier);
        }

        private ServiceDeclaration<?> transformDeclaration(ServiceDeclaration<?> declaration) {
            ServiceDeclaration<?> current = declaration;
            // transform declaration
            for (Service<ServiceDeclarationTransformer> transformerService : declarationTransformers) {
                final ServiceDeclarationTransformer transformer = transformerService.get();
                current = transformer.transform(current);
            }
            return current;
        }

        private <T> T transformInstance(List<Service<ServiceInstanceTransformer>> transformers,
                                        ServiceDeclaration<T> declaration,
                                        T instance) {
            T current = instance;
            for (Service<ServiceInstanceTransformer> transformerService : transformers) {
                // do not use a transformer to transform itself
                final ServiceInstanceTransformer instanceTransformer = transformerService.get();
                current = instanceTransformer.transform(declaration, current);
            }
            return current;
        }

        private boolean isResolving(String name, Type type) {
            return resolvingDeclarations.stream().anyMatch(d -> d.match(name, type));
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

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o == delegate;
        }
    }
}
