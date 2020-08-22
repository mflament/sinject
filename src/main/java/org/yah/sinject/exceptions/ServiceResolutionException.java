package org.yah.sinject.exceptions;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public class ServiceResolutionException extends RuntimeException {

    private final Type type;
    private final String name;

    public ServiceResolutionException(Type type, String message) {
        this(null, type, message, null);
    }

    public ServiceResolutionException(String name, Type type, String message) {
        this(name, type, message, null);
    }

    public ServiceResolutionException(String name, Type type, String message, Throwable cause) {
        super(String.format("Error resolving '%s': %s", definitionString(name, type), message), cause);
        this.type = Objects.requireNonNull(type, "type is null");
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    private static String definitionString(String name, Type type) {
        Objects.requireNonNull(type);
        if (name != null)
            return String.format("Service %s with type %s", name, type);
        return String.format("Service with type %s", type);
    }
}
