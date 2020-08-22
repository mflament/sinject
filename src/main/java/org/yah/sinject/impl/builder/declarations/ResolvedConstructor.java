package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.impl.builder.ServiceDependencies;
import org.yah.sinject.impl.builder.ResolvedDependencies;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class ResolvedConstructor<T> implements Comparable<ResolvedConstructor<T>> {
    private final CandidateConstuctor<T> constructor;
    private final ResolvedDependencies resolvedDependencies;

    ResolvedConstructor(CandidateConstuctor<T> constructor,
                        ResolvedDependencies resolvedDependencies) {
        this.constructor = Objects.requireNonNull(constructor, "constructor is null");
        this.resolvedDependencies = resolvedDependencies;
    }

    public CandidateConstuctor<T> getConstructor() {
        return constructor;
    }

    public ResolvedDependencies getResolvedDependencies() {
        return resolvedDependencies;
    }

    public boolean isResolved() {
        return resolvedDependencies.isResolved();
    }

    public ServiceDependencies getDependencies() {
        return constructor.getDependencies();
    }

    public int resolvedCount() {
        return resolvedDependencies.resolvedCount();
    }

    public T newInstance() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final Object[] params = resolvedDependencies.get();
        return constructor.newInstance(params);
    }

    @Override
    public int compareTo(ResolvedConstructor<T> o) {
        return Integer.compare(o.resolvedCount(), resolvedCount());
    }
}
