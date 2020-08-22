package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.ServiceResolver;
import org.yah.sinject.impl.builder.ServiceDependencies;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class CandidateConstuctor<T> {

    public static <T> CandidateConstuctor<T> create(Constructor<T> constructor) {
        ServiceDependencies dependencies = ServiceDependencies.builder()
                .addParameters(constructor.getParameters())
                .build();
        return new CandidateConstuctor<>(constructor, dependencies);
    }

    private final Constructor<T> constructor;
    private final ServiceDependencies dependencies;

    public CandidateConstuctor(Constructor<T> constructor,
                               ServiceDependencies dependencies) {
        this.constructor = Objects.requireNonNull(constructor, "constructor is null");
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies is null");
    }

    public ResolvedConstructor<T> resolve(ServiceResolver resolver) {
        return new ResolvedConstructor<>(this, dependencies.resolve(resolver));
    }

    @Override
    public String toString() {
        return "CandidateConstuctor{" +
                "constructor=" + constructor +
                ", dependencies=" + dependencies +
                '}';
    }

    public void ensureAccessible() {
        if (!constructor.canAccess(null))
            constructor.setAccessible(true);
    }

    T newInstance(Object[] params) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return constructor.newInstance(params);
    }

}
