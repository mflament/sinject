package org.yah.sinject.impl.builder;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.yah.sinject.Service;
import org.yah.sinject.exceptions.ServiceResolutionException;
import org.yah.sinject.ServiceDefinition;
import org.yah.sinject.ServiceResolver;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Optional;

import static org.yah.sinject.impl.DefaultServicesBuilder.optionalArgument;

public class ServiceDependency {

    public static ServiceDependency fromDefinition(ServiceDefinition definition, boolean optional) {
        return new ServiceDependency(definition.type(), definition.name(), optional);
    }

    public static ServiceDependency fromParameter(Parameter parameter) {
        Type type = parameter.getParameterizedType();
        boolean optional = false;
        if (TypeUtils.isAssignable(type, Optional.class)) {
            optional = true;
            type = optionalArgument(type);
        }
        return new ServiceDependency(type, parameter.getName(), optional);
    }

    private final Type type;
    private final String name;
    private final boolean optional;

    public ServiceDependency(Type type, String name, boolean optional) {
        this.type = type;
        this.name = name;
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", optional=" + optional +
                '}';
    }

    public ResolvedServiceDependency resolve(ServiceResolver resolver) {
        try {
            Service<?> resolved = resolver.service(name, type);
            return new ResolvedServiceDependency(this, resolved);
        } catch (ServiceResolutionException e) {
            try {
                Service<?> resolved = resolver.service(null, type);
                return new ResolvedServiceDependency(this, resolved);
            } catch (ServiceResolutionException e2) {
                return new ResolvedServiceDependency(this, e);
            }
        }
    }

}
