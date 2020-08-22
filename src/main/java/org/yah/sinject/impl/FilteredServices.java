package org.yah.sinject.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.yah.sinject.Service;
import org.yah.sinject.TypedServices;
import org.yah.sinject.Parametric;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

class FilteredServices<T> implements TypedServices<T> {

    public static <T> FilteredServices<T> create(Iterable<? extends Service<?>> delegate, Class<? super T> type) {
        return new FilteredServices<>(delegate, type);
    }

    public static <T> FilteredServices<T> create(Iterable<? extends Service<?>> delegate, Parametric<? super T> parametric) {
        return new FilteredServices<>(delegate, parametric.getType());
    }

    private final Iterable<? extends Service<?>> delegate;

    private final Type type;

    private FilteredServices(Iterable<? extends Service<?>> delegate, Type type) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is null");
        this.type = Objects.requireNonNull(type, "type is null");
    }

    @Override
    public <V> TypedServices<V> services(Class<? super V> type) {
        return new FilteredServices<>(this, type);
    }

    @Override
    public <V> TypedServices<V> services(Parametric<? super V> parametric) {
        return new FilteredServices<>(this, parametric.getType());
    }

    @Override
    @Nonnull
    public Iterator<Service<T>> iterator() {
        return new ServiceDefinitionIterator(delegate.iterator());
    }

    @Override
    public String toString() {
        return stream().collect(Collectors.toList()).toString();
    }

    private class ServiceDefinitionIterator implements Iterator<Service<T>> {
        private final Iterator<? extends Service<?>> delegate;
        private Service<T> next;

        public ServiceDefinitionIterator(Iterator<? extends Service<?>> delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate is null");
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                while (delegate.hasNext()) {
                    Service<?> current = delegate.next();
                    if (TypeUtils.isAssignable(current.type(), type)) {
                        //noinspection unchecked
                        next = (Service<T>) current;
                        break;
                    }
                }
                return next != null;
            }
            return true;
        }

        @Override
        public Service<T> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            Service<T> res = next;
            next = null;
            return res;
        }

    }
}
