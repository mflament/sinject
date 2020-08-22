package org.yah.sinject.builder;

public interface InstanceSupplier<T> {
    T get() throws Exception;
}
