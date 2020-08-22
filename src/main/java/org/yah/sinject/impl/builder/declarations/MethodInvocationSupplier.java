package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.impl.builder.ResolvedDependencies;

import java.lang.reflect.InvocationTargetException;

public class MethodInvocationSupplier<T> implements InstanceSupplier<T> {

    private final AnnotatedMethod method;
    private final ResolvedDependencies resolvedDependencies;

    public MethodInvocationSupplier(AnnotatedMethod method, ResolvedDependencies resolvedDependencies) {
        this.method = method;
        this.resolvedDependencies = resolvedDependencies;
        method.ensureAccessible();
    }

    @Override
    public T get() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(resolvedDependencies);
    }

}
