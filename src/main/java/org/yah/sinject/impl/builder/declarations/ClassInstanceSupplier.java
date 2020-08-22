package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.builder.InstanceSupplier;

import java.util.Objects;

public class ClassInstanceSupplier<T> implements InstanceSupplier<T> {
    private final ResolvedConstructor<T> constructor;

    public ClassInstanceSupplier(ResolvedConstructor<T> constructor) {
        this.constructor = Objects.requireNonNull(constructor, "constructor is null");
    }

    @Override
    public T get() throws Exception {
        return constructor.newInstance();
    }
}
