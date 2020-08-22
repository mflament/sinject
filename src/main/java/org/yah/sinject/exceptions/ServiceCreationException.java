package org.yah.sinject.exceptions;


import org.yah.sinject.ServiceDefinition;

public class ServiceCreationException extends RuntimeException {

    public ServiceCreationException(ServiceDefinition definition, String message) {
        this(definition, message, null);
    }

    public ServiceCreationException(ServiceDefinition definition, String message, Throwable cause) {
        super("Error creating service " + definition + ": " + message, cause);
    }

    public ServiceCreationException(String message) {
        super(message);
    }

    public ServiceCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
