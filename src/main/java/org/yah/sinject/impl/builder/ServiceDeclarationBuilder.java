package org.yah.sinject.impl.builder;

import org.yah.sinject.Parametric;
import org.yah.sinject.annotations.Service;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.builder.ServiceInstanceFactory;
import org.yah.sinject.impl.builder.declarations.DefaultServiceDeclaration;
import org.yah.sinject.impl.builder.declarations.MethodServiceDeclaration;
import org.yah.sinject.impl.DefaultServicesBuilder;
import org.yah.sinject.impl.builder.declarations.AnnotatedMethod;
import org.yah.sinject.impl.builder.declarations.ClassInstanceDeclaration;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

import static org.apache.commons.lang3.reflect.TypeUtils.getRawType;

public final class ServiceDeclarationBuilder<T> {

    public static <T> ServiceDeclarationBuilder<T> create(DefaultServicesBuilder servicesBuilder, Class<T> type) {
        return new ServiceDeclarationBuilder<>(servicesBuilder, type);
    }

    public static <T> ServiceDeclarationBuilder<T> create(DefaultServicesBuilder servicesBuilder, Parametric<T> parametric) {
        return new ServiceDeclarationBuilder<>(servicesBuilder, parametric.getType());
    }

    private final Type serviceType;
    private final DefaultServicesBuilder servicesBuilder;

    private String name;
    private int priority;

    private ServiceInstanceFactory<T> instanceFactory;
    private ServiceDependencies dependencies = ServiceDependencies.EMPTY;

    ServiceDeclarationBuilder(DefaultServicesBuilder servicesBuilder, Type serviceType) {
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType is null");
        this.servicesBuilder = Objects.requireNonNull(servicesBuilder, "servicesBuilder is null");
    }

    public ServiceDeclarationBuilder<T> withName(String name) {
        this.name = name;
        return this;
    }

    public ServiceDeclarationBuilder<T> withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public ServiceDeclarationBuilder<T> withInstanceFactory(ServiceInstanceFactory<T> instanceFactory) {
        this.instanceFactory = instanceFactory;
        return this;
    }

    public ServiceDeclarationBuilder<T> withInstance(T instance) {
        this.instanceFactory = dependencies -> instance;
        return this;
    }

    public ServiceDeclarationBuilder<T> withDependencies(ServiceDependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies is null");
        return this;
    }

    public DefaultServicesBuilder register() {
        if (instanceFactory == null) {
            //noinspection unchecked
            final Class<T> serviceClass = (Class<T>) getRawType(serviceType, null);
            final ServiceDeclaration<T> serviceDeclaration = ClassInstanceDeclaration.builder(serviceClass)
                    .withPriority(priority)
                    .withName(name)
                    .build();
            servicesBuilder.register(serviceDeclaration);

            // walk up class hierarchy to collect any annotated methods
            Class<?> current = serviceClass;
            while (current != null) {
                Arrays.stream(current.getDeclaredMethods())
                        .map(this::createAnnotatedMethod)
                        .filter(Objects::nonNull)
                        .map(method -> MethodServiceDeclaration.create(serviceDeclaration, method))
                        .forEach(servicesBuilder::register);
                current = current.getSuperclass();
            }
            return servicesBuilder;
        } else {
            final ServiceDeclaration<?> declaration = DefaultServiceDeclaration.<T>builder(serviceType)
                    .withName(name)
                    .withPriority(priority)
                    .withDependencies(dependencies)
                    .withFactory(instanceFactory)
                    .build();
            return servicesBuilder.register(declaration);
        }
    }

    private AnnotatedMethod createAnnotatedMethod(Method method) {
        final Service annotation = method.getAnnotation(Service.class);
        if (annotation != null)
            return new AnnotatedMethod(method, annotation);
        return null;
    }


}
