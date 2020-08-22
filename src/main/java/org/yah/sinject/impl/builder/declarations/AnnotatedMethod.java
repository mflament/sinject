package org.yah.sinject.impl.builder.declarations;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.yah.sinject.annotations.Service;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.impl.builder.ServiceDependencies;
import org.yah.sinject.impl.DefaultServicesBuilder;
import org.yah.sinject.impl.builder.ResolvedDependencies;
import org.yah.sinject.impl.builder.ServiceDependency;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.reflect.TypeUtils.isAssignable;

public final class AnnotatedMethod {
    private final Method method;
    private final Service annotation;
    private final Type serviceType;
    private final boolean optional;
    private final boolean serviceClass;

    public AnnotatedMethod(Method method, Service annotation) {
        this.method = Objects.requireNonNull(method, "method is null");
        this.annotation = Objects.requireNonNull(annotation, "annotation is null");

        Type type = method.getGenericReturnType();
        optional = isAssignable(method.getGenericReturnType(), Optional.class);
        if (optional)
            type = DefaultServicesBuilder.optionalArgument(type);

        if (!(type instanceof Class || type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Unhandled return type '" + type.getClass().getName() +
                    "' for " + method);
        }

        if (isAssignable(type, Class.class)) {
            // for class return value, create an injected constructor
            type = classArgument(type);
            if (type instanceof WildcardType)
                type = ((WildcardType)type).getUpperBounds()[0];
            serviceClass = true;
        } else {
            serviceClass = false;
        }
        serviceType = type;
    }

    public Method getMethod() {
        return method;
    }

    public Service getAnnotation() {
        return annotation;
    }

    public Type getServiceType() {
        return serviceType;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isServiceClass() {
        return serviceClass;
    }

    public ServiceDependencies createDependencies(ServiceDeclaration<?> instanceDeclaration) {
        final ServiceDependencies.Builder builder = ServiceDependencies.builder();
        if (isNotStatic())
            builder.addDependencies(ServiceDependency.fromDefinition(instanceDeclaration, false));
        return builder.addParameters(method.getParameters()).build();
    }

    public String getName() {
        String name = trimToNull(annotation.name());
        if (name == null)
            name = trimToNull(annotation.value());
        if (name == null)
            name = method.getName();
        return name;
    }

    public boolean isNotStatic() {
        return !Modifier.isStatic(method.getModifiers());
    }

    public int getPriority() {
        return annotation.priority();
    }

    public <T> T invoke(ResolvedDependencies resolvedDependencies) throws InvocationTargetException, IllegalAccessException {
        Object[] dependencies = resolvedDependencies.get();
        Object instance = null;
        if (isNotStatic()) {
            instance = dependencies[0];
            dependencies = Arrays.copyOfRange(dependencies, 1, dependencies.length);
        }
        //noinspection unchecked
        return (T) method.invoke(instance, dependencies);
    }

    public void ensureAccessible() {
        method.setAccessible(true);
    }

    private static Type classArgument(Type type) {
        final Map<TypeVariable<?>, Type> arguments = TypeUtils.getTypeArguments(type, Class.class);
        if (arguments.isEmpty())
            return Object.class;
        return arguments.values().iterator().next();
    }

}
