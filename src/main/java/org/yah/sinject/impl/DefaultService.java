package org.yah.sinject.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yah.sinject.Service;
import org.yah.sinject.ServiceDefinition;
import org.yah.sinject.Services;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.exceptions.ServiceCreationException;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public final class DefaultService<T> implements Service<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultService.class);

    private final Services source;
    private final ServiceDeclaration<T> declaration;
    private final InstanceSupplier<? extends T> instanceSupplier;

    protected T instance;

    public DefaultService(Services source, ServiceDeclaration<T> declaration,
                          InstanceSupplier<? extends T> instanceSupplier) {
        this.source = Objects.requireNonNull(source, "source is null");
        this.declaration = Objects.requireNonNull(declaration, "declaration is null");
        this.instanceSupplier = Objects.requireNonNull(instanceSupplier, "factory is null");
    }

    @Override
    public Type type() {
        return declaration.type();
    }

    @Override
    public String name() {
        return declaration.name();
    }

    @Override
    public int priority() {
        return declaration.priority();
    }

    public ServiceDeclaration<T> getDeclaration() {
        return declaration;
    }

    @Override
    public synchronized T get() {
        if (instance == null) {
            try {
                instance = instanceSupplier.get();
            } catch (Exception e) {
                throw new ServiceCreationException(this, "error creating service instance", e);
            }
        }
        return instance;
    }

    @Override
    public synchronized Optional<T> peek() {
        if (instance == null)
            return Optional.empty();
        return Optional.of(get());
    }

    @Override
    public Services getSource() {
        return source;
    }

    @Override
    public String toString() {
        return ServiceDefinition.toString(this);
    }

    @Override
    public void close() {
        if (instance != null && instance instanceof AutoCloseable) {
            try {
                ((AutoCloseable) instance).close();
            } catch (Exception e) {
                LOGGER.error("Error closing service {}", this, e);
            }
        }
    }
}
