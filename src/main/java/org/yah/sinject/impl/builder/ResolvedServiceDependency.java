package org.yah.sinject.impl.builder;

import org.yah.sinject.Service;
import org.yah.sinject.exceptions.ServiceResolutionException;

import java.util.Objects;
import java.util.Optional;

public class ResolvedServiceDependency {
    private final ServiceDependency dependency;
    private final Service<?> service;
    private final ServiceResolutionException resolutionException;

    public ResolvedServiceDependency(ServiceDependency dependency, Service<?> service) {
        this.dependency = Objects.requireNonNull(dependency, "dependency is null");
        this.service = service;
        this.resolutionException = null;
    }

    public ResolvedServiceDependency(ServiceDependency dependency, ServiceResolutionException resolutionException) {
        this.dependency = Objects.requireNonNull(dependency, "dependency is null");
        this.service = null;
        this.resolutionException = Objects.requireNonNull(resolutionException, "resolutionException is null");
    }

    public boolean isResolved() {
        return service != null || dependency.isOptional();
    }

    public boolean isOptional() {
        return dependency.isOptional();
    }

    public Optional<ServiceResolutionException> getResolutionException() {
        return Optional.ofNullable(resolutionException);
    }

    @Override
    public String toString() {
        return "ResolvedServiceDependency{" +
                "dependency=" + dependency +
                ", service=" + service +
                '}';
    }

    public Object get() {
        if (service == null) {
            if (isOptional())
                return Optional.empty();
            // should not happen, dependency resolution check is done at build time
            throw new IllegalStateException("unresolved dependency: " + this);
        }
        return isOptional() ? Optional.of(service.get()) : service.get();
    }

}
