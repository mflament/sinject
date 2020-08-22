package org.yah.sinject.impl.builder;

import org.yah.sinject.ServiceResolver;

import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceDependencies {

    public static final ServiceDependencies EMPTY = new ServiceDependencies(Collections.emptyList());

    private final List<ServiceDependency> dependencies;

    private ServiceDependencies(Collection<? extends ServiceDependency> dependencies) {
        this.dependencies = List.copyOf(dependencies);
    }

    public ResolvedDependencies resolve(ServiceResolver resolver) {
        final List<ResolvedServiceDependency> resolved = dependencies.stream()
                .map(d -> d.resolve(resolver))
                .collect(Collectors.toList());
        return new ResolvedDependencies(resolved);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<ServiceDependency> dependencies = new ArrayList<>();

        private Builder() {
        }

        public Builder withDependencies(List<ServiceDependency> dependencies) {
            this.dependencies.clear();
            this.dependencies.addAll(dependencies);
            return this;
        }

        public Builder addDependencies(ServiceDependency... dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        public Builder addParameters(Parameter[] parameters) {
            Arrays.stream(parameters).map(ServiceDependency::fromParameter).forEach(dependencies::add);
            return this;
        }

        public ServiceDependencies build() {
            return new ServiceDependencies(dependencies);
        }
    }
}
