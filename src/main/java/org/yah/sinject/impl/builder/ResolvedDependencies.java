package org.yah.sinject.impl.builder;

import org.yah.sinject.exceptions.ServiceResolutionException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ResolvedDependencies {

    private final List<ResolvedServiceDependency> resolvedDependencies;

    private final boolean resolved;
    private final int resolvedCount;

    public ResolvedDependencies(Collection<ResolvedServiceDependency> resolvedDependencies) {
        this.resolvedDependencies = List.copyOf(resolvedDependencies);
        resolvedCount = (int)resolvedDependencies.stream().filter(ResolvedServiceDependency::isResolved).count();
        resolved = resolvedCount == resolvedDependencies.size();
    }

    public List<ResolvedServiceDependency> getResolvedDependencies() {
        return resolvedDependencies;
    }

    /**
     * @return true if all non optional dependencies are resolved
     */
    public boolean isResolved() {
        return resolved;
    }

    public int resolvedCount() {
        return resolvedCount;
    }

    public Object[] get() {
        return resolvedDependencies.stream().map(ResolvedServiceDependency::get).toArray(Object[]::new);
    }

    public ServiceResolutionException getResolutionException() {
        if (resolved)
            throw new IllegalStateException("dependencies are resolved");
        return resolvedDependencies.stream()
                .map(ResolvedServiceDependency::getResolutionException)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("not resolution exception found"));
    }
}
