package org.yah.sinject;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Type;

public interface ServiceDefinition extends Comparable<ServiceDefinition> {

    static String toString(ServiceDefinition definition) {
        return String.format("{name: \"%s\", type: \"%s\", priority: %d}",
                definition.name(),
                definition.type().getTypeName(),
                definition.priority());
    }

    Type type();

    String name();

    int priority();

    default boolean isAssignableTo(Type type) {
        return TypeUtils.isAssignable(this.type(), type);
    }

    default boolean match(String name, Type type) {
        if (name == null)
            return isAssignableTo(type);
        return name().equals(name) && isAssignableTo(type);
    }

    default boolean match(ServiceDefinition other) {
        return match(other.name(), other.type());
    }

    /**
     * Sort service definitions by priority in ascending order.
     * Lowest priority will be handled first
     */
    @Override
    default int compareTo(ServiceDefinition o) {
        return Integer.compare(priority(), o.priority());
    }

}
