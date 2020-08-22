package org.yah.sinject.exceptions;


import org.yah.sinject.ServiceDefinition;

public class UnresolvedDependencyException extends ServiceCreationException {

    public UnresolvedDependencyException(ServiceDefinition definition, ServiceResolutionException cause) {
        super(definition, "error resolving dependency", cause);
    }

    @Override
    public synchronized ServiceResolutionException getCause() {
        return (ServiceResolutionException) super.getCause();
    }
}
