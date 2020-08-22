package org.yah.sinject.impl;

import org.yah.sinject.ServiceDefinition;

import java.lang.reflect.Type;
import java.util.Objects;

public class DefaultServiceDefinition implements ServiceDefinition {
    protected final String name;
    protected final Type type;
    protected final int priority;

    public DefaultServiceDefinition(Type type, String name, int priority) {
        this.type = Objects.requireNonNull(type, "type is null");
        this.name = Objects.requireNonNull(name, "name is null");
        this.priority = priority;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String toString() {
        return ServiceDefinition.toString(this);
    }

}
